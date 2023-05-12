package dk.abandonship.gui.controller;

import dk.abandonship.Main;

import dk.abandonship.entities.Role;
import dk.abandonship.entities.User;
import dk.abandonship.gui.controller.PopUpController.AddEditUserController;
import dk.abandonship.gui.model.UserModel;
import dk.abandonship.state.LoggedInUserState;
import dk.abandonship.utils.DefaultRoles;

import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;

import javafx.scene.control.Button;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Objects;
import java.util.ResourceBundle;

public class UsersViewController implements Initializable {

    private final UserModel userModel;
    public TableView<User> userTableView;
    public TableColumn<User, String> nameColumn;
    public TableColumn<User, String> emailColumn;
    public TableColumn<User, String> phoneColumn;
    public HBox buttonsHBox;
    private Button deleteUserButton, editUserButton;

    public UsersViewController() {
        userModel = new UserModel();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        userTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> updateSelectedDisabledButtons());

        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));

        ContextMenu roleAssign = new ContextMenu();
        roleAssign.setStyle("-fx-background-color: #3D445C;");

        try {
            for (var r : userModel.getAllRoles()) {
                var role = new CheckMenuItem(r.getName());
                roleAssign.getItems().add(role);
                role.setOnAction(event -> handleAssignRole(r));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        userTableView.setRowFactory(tv -> {
            var row = new TableRow<User>();
            row.setContextMenu(roleAssign);
            return row;
        });
        userTableView.setItems(userModel.getUserObserveableList());

        buttonsHBox.setSpacing(10);

        if(LoggedInUserState.getInstance().getLoggedInUser().hasRole(DefaultRoles.ADMIN, DefaultRoles.PROJECTMANAGER)) {
            Button addUserButton = new Button("Add User");
            addUserButton.setOnAction(event -> {
                try {
                    handleAddEditUser(null);
                } catch (IOException e) {
                    System.err.println("Could not open window!");
                    e.printStackTrace();
                }
            });
            buttonsHBox.getChildren().add(addUserButton);

            editUserButton = new Button("Edit User");
            editUserButton.setOnAction(event -> {
                var selectedItem = userTableView.getSelectionModel().getSelectedItem();
                if(selectedItem == null) return;

                try {
                    handleAddEditUser(selectedItem);
                } catch (IOException e) {
                    System.err.println("Could not open window!");
                    throw new RuntimeException(e);
                }

            });
            buttonsHBox.getChildren().add(editUserButton);

            deleteUserButton = new Button("Delete User");
            deleteUserButton.setOnAction(event -> handleDeleteUser());
            buttonsHBox.getChildren().add(deleteUserButton);
        }

        updateSelectedDisabledButtons();
    }

    private void updateSelectedDisabledButtons() {
        var selectedItem = userTableView.getSelectionModel().getSelectedItem();

        if(deleteUserButton != null) {
            deleteUserButton.setDisable(selectedItem == null);
        }

        if(editUserButton != null) {
            editUserButton.setDisable(selectedItem == null);
        }
    }

    public void handleAddEditUser(User user) throws IOException {
        Stage popupStage = new Stage();

        FXMLLoader loader = new FXMLLoader(Main.class.getResource("gui/view/PopUps/NewUser.fxml"));

        Parent root = loader.load();
        AddEditUserController controller = loader.getController();

        if(user != null) {
            controller.editMode(user);
        }

        Scene popupScene = new Scene(root);

        popupStage.setScene(popupScene);
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initStyle(StageStyle.UNDECORATED);

        popupStage.showAndWait();

        User result = controller.getResult();
        if(result == null) return;

        try {
            if(user == null) {
                userModel.addUser(result);
            } else {
                boolean wasEdited = userModel.editUser(user);
                if(wasEdited) userTableView.refresh();
                userTableView.getSelectionModel().clearSelection();
            }
        } catch (SQLException e) {
            System.err.println("Could not add user!");
            e.printStackTrace();
        }
    }

    public void handleDeleteUser() {
        var selectedItem = userTableView.getSelectionModel().getSelectedItem();
        if(selectedItem == null) return;

        try {
            userModel.deleteUser(selectedItem);
        } catch (SQLException e) {
            System.err.println("Could not delete user!");
            e.printStackTrace();
        }
    }

    public void handleAssignRole(Role role) {
        try {
            userModel.addRole(userTableView.getSelectionModel().getSelectedItem(), role);
        } catch (SQLException e) {
            System.err.println("Could not add role to user!");
            e.printStackTrace();
        }
    }

}
