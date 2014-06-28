package org.openscience.cdk;

import de.erichseifert.vectorgraphics2d.PDFGraphics2D;
import de.erichseifert.vectorgraphics2d.SVGGraphics2D;
import net.sf.jniinchi.INCHI_RET;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.inchi.InChIToStructure;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.io.CMLReader;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.render.AtomContainerIcon;
import org.openscience.cdk.render.Coloring;
import org.openscience.cdk.silent.AtomContainer;
import org.openscience.cdk.silent.ChemFile;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;
import uk.ac.cam.ch.wwmm.opsin.NameToStructure;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Manipulation of the model and marshalling input/output. The main method here
 * is {@link #loadContent(String)} that takes some arbitrary content and tries
 * to load whatever was input. 
 * 
 * @author John May
 */
final class Controller {

    // chemobj builders
    private final IChemObjectBuilder bldr = SilentChemObjectBuilder.getInstance();

    // IO factories
    private final SmilesParser              smipar = new SmilesParser(bldr);
    private final StructureDiagramGenerator sdg    = new StructureDiagramGenerator();
    private final NameToStructure           opsin  = NameToStructure.getInstance();
    

    Model model;
    List<View> views = new ArrayList<View>();

    Controller(Model model) {
        this.model = model;
        this.sdg.setUseTemplates(false);
    }

    boolean loadContent(String content) {
        content = content.trim();
        if (content.isEmpty()) {
            model = null;
            fireModelChanged();
        }
        else if (content.contains("InChI")) {
            return loadInChI(content);
        }
        else if (content.contains("V2000")) {
            return loadMolfile(content);
        }
        else if (content.contains("<cml")) {
            return loadCml(content);
        }
        else if (content.startsWith("~") || new File(content).exists()) {
            content = content.replaceAll("^~", System.getProperty("user.home"));
            if (loadCmlFromPath(content))
                return true;
            if (loadMolfileFromPath(content))
                return true;
        }
        else if (loadSmi(content)) {
            return true;
        }
        else if (loadName(content)) {
            return true;
        }
        return false;
    }

    boolean loadCmlFromPath(String path) {
        CMLReader cmlr = null;
        try {
            cmlr = new CMLReader(new FileInputStream(path));
            IChemFile chemfile = cmlr.read(new ChemFile());
            IAtomContainer container = new AtomContainer();
            for (IAtomContainer chemfileContainer : ChemFileManipulator.getAllAtomContainers(chemfile))
                container.add(chemfileContainer);
            model = Model.create(container);
            fireModelChanged();
            return true;
        } catch (IOException e) {
            reportError("Could not load CML input: " + e.getMessage());
            return false;
        } catch (CDKException e) {
            System.err.println(e.getMessage());
            reportError("Could not load CML input: " + e.getMessage());
            return false;
        } finally {
            try {
                if (cmlr != null) cmlr.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }

        }
    }

    boolean loadCml(String content) {
        CMLReader cmlr = null;
        try {
            cmlr = new CMLReader();
            cmlr.setReader(new ByteArrayInputStream(content.getBytes()));
            IChemFile chemfile = cmlr.read(new ChemFile());
            IAtomContainer container = new AtomContainer();
            for (IAtomContainer chemfileContainer : ChemFileManipulator.getAllAtomContainers(chemfile))
                container.add(chemfileContainer);
            model = Model.create(container);
            fireModelChanged();
            return true;
        } catch (CDKException e) {
            System.err.println(e.getMessage());
            reportError("Could not load CML input: " + e.getMessage());
            return false;
        } finally {
            try {
                if (cmlr != null) cmlr.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }

        }
    }

    boolean loadMolfileFromPath(String path) {
        MDLV2000Reader mdlr = null;
        try {
            mdlr = new MDLV2000Reader(new FileReader(path));
            IAtomContainer container = mdlr.read(new AtomContainer(0, 0, 0, 0));
            model = Model.create(container);
            fireModelChanged();
            return true;
        } catch (IOException e) {
            reportError("Could not load molfile input: " + e.getMessage());
            return false;
        } catch (CDKException e) {
            System.err.println(e.getMessage());
            reportError("Could not load molfile input: " + e.getMessage());
            return false;
        } finally {
            try {
                if (mdlr != null) mdlr.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }

        }
    }

    boolean loadMolfile(String content) {
        MDLV2000Reader mdlr = null;
        try {
            mdlr = new MDLV2000Reader(new StringReader(content));
            IAtomContainer container = mdlr.read(new AtomContainer(0, 0, 0, 0));
            model = Model.create(container);
            fireModelChanged();
            return true;
        } catch (CDKException e) {
            System.err.println(e.getMessage());
            reportError("Could not load molfile input: " + e.getMessage());
            return false;
        } finally {
            try {
                if (mdlr != null) mdlr.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }

        }
    }
    
    boolean loadSmi(String smi) {
        try {
            return loadLinearNotation(smipar.parseSmiles(smi));
        } catch (InvalidSmilesException e) {
            reportError("Could not load SMILES input: " + e.getMessage());
            return false;
        }
    }

    boolean loadName(String name) {
        String smi = opsin.parseToSmiles(name);

        if (smi == null) {
            reportError("Chemical name could not be interpreted");
            return false;
        }
        else {
            return loadSmi(smi);
        }
    }

    boolean loadInChI(String inchi) {
        try {
            InChIGeneratorFactory igf = InChIGeneratorFactory.getInstance();
            InChIToStructure inchiPar = igf.getInChIToStructure(inchi, bldr);
            if (inchiPar.getReturnStatus() != INCHI_RET.OKAY && inchiPar.getReturnStatus() != INCHI_RET.WARNING)
                return false;
            return loadLinearNotation(inchiPar.getAtomContainer());
        } catch (CDKException e) {
            reportError("Could not load InChI input: " + e.getMessage());
            return false;
        }
    }

    boolean loadLinearNotation(IAtomContainer container) {
        try {
            layout(container);
            model = Model.create(container);
            fireModelChanged();
            return true;
        } catch (CDKException e) {
            reportError("Could not layout SMILES input: " + e.getMessage());
            return false;
        }
    }
    
    File ensureExtension(File f, String extension) {
        if (f.getName().endsWith(extension))
            return f;
        return new File(f.getPath() + extension);
    }
    
    void exportToPDF(File f, double theta) {
        if (model == null || model.container == null)
            return;
        f = ensureExtension(f, ".pdf");
        Coloring coloring = Coloring.BLACK;
        AtomContainerIcon icon = new AtomContainerIcon(model.rotatedByDegree(theta), coloring);
        Rectangle2D bounds = icon.bounds();
        Rectangle view   = new Rectangle((int) (bounds.getWidth() * 100), (int) (bounds.getHeight() * 100));
        PDFGraphics2D g2 = new PDFGraphics2D(view.x, view.y, view.width, view.height);
        g2.setBackground(coloring.bgColor());
        icon.render(g2, view);
        try {
            FileWriter fw = new FileWriter(f);
            fw.write(g2.toString());
            fw.close();
        }catch (IOException ex) {
            reportError("Could not save PDF");
        }
    }

    void exportToSVG(File f, double theta) {
        if (model == null || model.container == null)
            return;
        f = ensureExtension(f, ".svg");
        Coloring coloring = Coloring.BLACK;
        AtomContainerIcon icon = new AtomContainerIcon(model.rotatedByDegree(theta), coloring);
        Rectangle2D bounds = icon.bounds();
        Rectangle view   = new Rectangle((int) (bounds.getWidth() * 100), (int) (bounds.getHeight() * 100));
        SVGGraphics2D g2 = new SVGGraphics2D(view.x, view.y, view.width, view.height);
        g2.setBackground(coloring.bgColor());
        icon.render(g2, view);
        try {
            FileWriter fw = new FileWriter(f);
            fw.write(g2.toString());
            fw.close();
        }catch (IOException ex) {
            reportError("Could not save SVG");
        }

    }

    void layout(IAtomContainer container) throws CDKException {
        sdg.setMolecule(container, false);
        sdg.generateCoordinates();
    }

    void reportError(String err) {
        // stderr for now
        System.err.println(err);
    }
    
    void addView(View view) {
        views.add(view);
    }

    void fireModelChanged() {
        // notify views
        for (View view : views)
            view.update();
                 
    }
}
