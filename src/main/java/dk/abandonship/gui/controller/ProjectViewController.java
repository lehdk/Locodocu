package dk.abandonship.gui.controller;

import dk.abandonship.Main;
import dk.abandonship.entities.Project;
import dk.abandonship.gui.controller.PopUpController.AssignTechController;
import dk.abandonship.gui.controller.PopUpController.CreateProjectView;
import dk.abandonship.gui.controller.PopUpController.OldProjectController;
import dk.abandonship.gui.model.ProjectModel;
import dk.abandonship.state.LoggedInUserState;
import dk.abandonship.utils.ControllerAssistant;
import dk.abandonship.utils.DefaultRoles;
import javafx.animation.PauseTransition;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ProjectViewController implements Initializable {

    @FXML
    private CheckBox onlyShowAssigned;
    @FXML
    private TextField fieldSearch;
    @FXML
    private TableColumn<Project, String> projectName, projectAddress, projectPostalCode, customerName, docCount, createdAt;
    @FXML
    private HBox buttonsHBox;

    private Button openProjectButton, assignedTechniciansButton;

    @FXML
    private TableView<Project> projectTableView;

    private ProjectModel projectModel;

    private final ControllerAssistant controllerAssistant;

    public ProjectViewController() {
        controllerAssistant = ControllerAssistant.getInstance();
        try {
            projectModel = new ProjectModel();
        } catch (Exception e) {
            controllerAssistant.displayError(e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        openProjectButton = new Button("Open Project");
        openProjectButton.setDisable(true);
        openProjectButton.setPrefWidth(Region.USE_COMPUTED_SIZE);
        openProjectButton.setPrefHeight(Region.USE_COMPUTED_SIZE);
        openProjectButton.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEvent -> openProject(projectTableView.getSelectionModel().getSelectedItem()));
        buttonsHBox.getChildren().add(openProjectButton);

        assignedTechniciansButton = new Button("Assign Technicians");
        assignedTechniciansButton.setDisable(true);
        assignedTechniciansButton.setPrefWidth(Region.USE_COMPUTED_SIZE);
        assignedTechniciansButton.setPrefHeight(Region.USE_COMPUTED_SIZE);
        assignedTechniciansButton.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEvent -> assignTechnicians(projectTableView.getSelectionModel().getSelectedItem()));

        projectTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        projectTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            openProjectButton.setDisable(newSelection == null);
            assignedTechniciansButton.setDisable(newSelection == null);
        });

        if (LoggedInUserState.getInstance().getLoggedInUser().hasRole(DefaultRoles.PROJECTMANAGER)) {
            setNewProjectBtn();
            buttonsHBox.getChildren().add(assignedTechniciansButton);
        }

        buttonsHBox.setSpacing(15);
        setProjects();

        var pauseTransition = new PauseTransition(Duration.millis(150));
        fieldSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            pauseTransition.setOnFinished(e -> filterProjects());
            pauseTransition.playFromStart();
        });


        if (!LoggedInUserState.getInstance().getLoggedInUser().hasRole(DefaultRoles.PROJECTMANAGER) || !LoggedInUserState.getInstance().getLoggedInUser().hasRole(DefaultRoles.SALESPERSON)) {
            onlyShowAssigned.setDisable(true);
            onlyShowAssigned.setVisible(false);
        } else {
            onlyShowAssigned.selectedProperty().addListener(((observable, oldValue, newValue) -> filterProjects()));
        }
    }

    private void setNewProjectBtn() {

        Button btn = new Button("Add New Project");
        btn.setPrefWidth(Region.USE_COMPUTED_SIZE);
        btn.setPrefHeight(Region.USE_COMPUTED_SIZE);
        btn.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEvent -> addProject());
        buttonsHBox.getChildren().add(btn);
    }

    /**
     * sets the value of the tableview with data from observable list over projects in model
     */
    private void setProjects() {
        projectName.setCellValueFactory(new PropertyValueFactory<>("name"));
        projectAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        projectPostalCode.setCellValueFactory(new PropertyValueFactory<>("postalCode"));

        customerName.setCellValueFactory(new PropertyValueFactory<>("customer"));
        docCount.setCellValueFactory(new PropertyValueFactory<>("documentations"));
        createdAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        filterProjects();
    }

    public void isOld() {
        List<Project> oldProjects = new ArrayList<>();

        LocalDate now = LocalDate.now();

        for (var v : projectModel.getProjectObservableList()) {

            if (v.getCreatedAt().toLocalDateTime().isBefore(now.minusMonths(48).atStartOfDay())) {
                oldProjects.add(v);
            }
        }

        if (!oldProjects.isEmpty()) openOldProjectPop(oldProjects);
    }

    /**
     * open createProject view in a pop-up box
     */
    private void addProject() {
        try {
            Stage popupStage = new Stage();

            FXMLLoader loader = new FXMLLoader(Main.class.getResource("gui/view/PopUps/CreateProjectView.fxml"));
            Parent root = loader.load();
            var controller = (CreateProjectView) loader.getController();
            Scene popupScene = new Scene(root);

            popupStage.setScene(popupScene);
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initStyle(StageStyle.UNDECORATED);
            popupStage.showAndWait();

            var result = controller.getResult();

            if (result == null) return;

            projectModel.createProject(result);

            projectTableView.refresh();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Open project and sets it in center view
     *
     * @param project project that should be opened
     */
    private void openProject(Project project) {
        try {
            var controller = (DocViewController) controllerAssistant.setCenterFX("DocView");
            controller.setProject(project);
        } catch (Exception e) {
            controllerAssistant.displayError(e);
        }
    }

    /**
     * open assignTechVIew in a pop-up box
     *
     * @param project project technicians should be assigned to
     */
    private void assignTechnicians(Project project) {
        try {
            Stage popupStage = new Stage();

            FXMLLoader loader = new FXMLLoader(Main.class.getResource("gui/view/PopUps/AssignTechView.fxml"));
            Parent root = loader.load();
            Scene popupScene = new Scene(root);

            AssignTechController techController = loader.getController();
            techController.setProject(project);

            popupStage.setScene(popupScene);
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initStyle(StageStyle.UNDECORATED);
            popupStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openOldProjectPop(List<Project> oldProjects) {
        try {
            Stage popupStage = new Stage();

            FXMLLoader loader = new FXMLLoader(Main.class.getResource("gui/view/PopUps/OldProjectPopUp.fxml"));
            Parent root = loader.load();
            Scene popupScene = new Scene(root);

            OldProjectController oldController = loader.getController();
            oldController.setOldProjects(oldProjects);

            popupStage.setScene(popupScene);
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initStyle(StageStyle.UNDECORATED);
            popupStage.showAndWait();
        } catch (Exception e) {
            controllerAssistant.displayError(e);
        }
    }

    /**
     * Set doubleClick to open a doc on the project Tableview
     *
     * @param mouseEvent click from mouse on an item in table
     */
    public void openItem(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2 && projectTableView.getSelectionModel().getSelectedItem() != null) { //Checking double click
            openProject(projectTableView.getSelectionModel().getSelectedItem());
        }
    }

    /**
     * Refreshes the projects with the selected filters
     */
    public void filterProjects() {
        String filterString = fieldSearch.getText().toLowerCase();
        var filteredList = new FilteredList<>(projectModel.getProjectObservableList());
        filteredList.setPredicate(project -> {
            // This must be first
            boolean isAssignedToUser = project.getAssignedTechnicians().contains(LoggedInUserState.getInstance().getLoggedInUser());
            if (onlyShowAssigned.isSelected() && !isAssignedToUser) return false;

            boolean containsProjectName = project.getName().toLowerCase().contains(filterString);
            if (containsProjectName) return true;

            boolean containsCustomerName = project.getCustomer().getName().toLowerCase().contains(filterString);
            if (containsCustomerName) return true;

            boolean containsPostalCode = project.getPostalCode().contains(filterString);
            if (containsPostalCode) return true;

            boolean containsProjectAddress = project.getAddress().toLowerCase().contains(filterString);
            if (containsProjectAddress) return true;

            boolean containsDocumentationName = project.getDocumentations().stream().anyMatch(d -> d.getName().toLowerCase().contains(filterString));
            if (containsDocumentationName) return true;

            return false;
        });

        var sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(projectTableView.comparatorProperty());
        projectTableView.setItems(sortedList);
    }
}
