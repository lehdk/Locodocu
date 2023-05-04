package dk.abandonship.dataaccess.interfaces;

import dk.abandonship.entities.Documentation;
import dk.abandonship.entities.Project;
import dk.abandonship.entities.documetationNodes.DocumentationLogInNode;
import dk.abandonship.entities.documetationNodes.DocumentationNode;
import dk.abandonship.entities.documetationNodes.DocumentationPictureNode;
import dk.abandonship.entities.documetationNodes.DocumentationTextFieldNode;
import javafx.scene.Node;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IDocumentationDAO {

    /**
     * Gets all the documentation for a project.
     * @param project The project you want the documentation from.
     * @return The documentation as a set
     * @throws SQLException
     */
    Set<Documentation> getDocumentationForProject(Project project) throws SQLException;

    List<DocumentationTextFieldNode> getDocumentationTextField(Documentation documentation) throws SQLException;

    List<DocumentationLogInNode> getDocumentationLogIn(Documentation documentation) throws SQLException;

    List<DocumentationPictureNode> getPictureNode(Documentation documentation) throws SQLException;

    DocumentationTextFieldNode createTextNode(String set, Documentation doc) throws SQLException;

    void updateTextNode( Map.Entry<Node, DocumentationNode> set) throws  SQLException;
}
