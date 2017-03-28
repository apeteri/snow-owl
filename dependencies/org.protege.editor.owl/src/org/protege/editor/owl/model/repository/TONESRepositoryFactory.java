package org.protege.editor.owl.model.repository;

import org.protege.editor.core.OntologyRepository;
import org.protege.editor.core.OntologyRepositoryFactory;


/**
 * Author: Matthew Horridge<br> The University Of Manchester<br> Information Management Group<br> Date:
 * 18-Oct-2008<br><br>
 */
public class TONESRepositoryFactory extends OntologyRepositoryFactory {


    public OntologyRepository createRepository() {
        return new TONESRepository();
    }


    public void initialise() throws Exception {
    }


    public void dispose() throws Exception {
    }
}