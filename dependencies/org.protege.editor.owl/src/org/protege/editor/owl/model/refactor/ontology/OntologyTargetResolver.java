package org.protege.editor.owl.model.refactor.ontology;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Author: drummond<br>
 * http://www.cs.man.ac.uk/~drummond/<br><br>
 * <p/>
 * The University Of Manchester<br>
 * Bio Health Informatics Group<br>
 * Date: Aug 21, 2008<br><br>
 */
public interface OntologyTargetResolver {

    Set<OWLOntology> resolve(OWLEntity entity, Set<OWLOntology> ontologies);
}