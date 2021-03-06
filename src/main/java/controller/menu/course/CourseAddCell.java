package controller.menu.course;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.event.ProgressEventType;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import data.mainapi.ESGIPocketProvider;
import data.mainapi.post.ESGIPocketProviderPost;
import data.model.Authentification;
import data.model.Course;
import data.model.SignedFile;
import data.model.User;
import data.model.credentials.CourseCredentials;
import interfaces.ApiListener;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;

public class CourseAddCell  {

    private String idTopic;
    private File fileToUpload;
    private String fileName = "";
    private static final String BUCKET_NAME = "esgipocket";
    private static final String BUCKET_ADDRESS = "https://s3.eu-west-3.amazonaws.com/esgipocket/";

    @FXML
    private AnchorPane anchorPane;

    @FXML
    private TextField filePathTextField;

    @FXML
    private TextField fileNameTextField;

    @FXML
    private Button chooseFileButton;

    @FXML
    private Button uploadButton;

    @FXML
    private Label progressLabel;

    public CourseAddCell(String idTopic){
        this.idTopic = idTopic;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/menu/course/CourseAddCell.fxml"));
        fxmlLoader.setController(this);
        try{
            anchorPane = fxmlLoader.load();
            anchorPane.getStylesheets().add(getClass().getResource("/menu/css/addCell.css").toExternalForm());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public AnchorPane getAnchorPane() {
        return anchorPane;
    }

    public void setAnchorPane(AnchorPane anchorPane) {
        this.anchorPane = anchorPane;
    }

    public void setAddCell(){
        chooseFile();
        uploadFile();
        uploadButton.setDisable(true);
    }

    public void chooseFile() {

        chooseFileButton.setOnMouseClicked(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Resource File");
            Node node = (Node) event.getSource();

            fileToUpload = fileChooser.showOpenDialog(node.getScene().getWindow());

            if (fileToUpload != null) {
                filePathTextField.setText(fileToUpload.getPath());
                fileNameTextField.setText(fileToUpload.getName());
                uploadButton.setDisable(false);
                progressLabel.setText("Modifier le nom fichier et cliquer sur le bouton \"Ajouter un cours\"");
            }
        });
    }

    public void uploadFile() {
        uploadButton.setOnMouseClicked(event -> {

            uploadButton.setDisable(true);
            chooseFileButton.setDisable(true);

            fileName = fileToUpload.getName();
            if (!fileName.contains(".")) {
                fileName = fileName + fileToUpload.getName().substring(fileToUpload.getName().lastIndexOf("."), fileToUpload.getName().length());
            }
            fileName = fileName.replaceAll("\\s+","_");
            signFile();
        });
    }

    public void signFile() {

        progressLabel.setText("Upload en cours");
        ESGIPocketProvider esgiPocketProvider = new ESGIPocketProvider(Authentification.getInstance().getToken());
        esgiPocketProvider.getSignedFile(Authentification.getInstance().getUser().getId() + "/" + fileName, "pdf", new ApiListener<SignedFile>() {
            @Override
            public void onSuccess(SignedFile response) {
                upload(response);
            }

            @Override
            public void onError(Throwable throwable) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        progressLabel.setText("Erreur lors de l'upload du fichier");
                    }
                });
            }
        });
    }


    public void upload(SignedFile signedFile) {


        String filePath = filePathTextField.getText();
        File f = new File(filePath);

        if (f.exists()) {
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_3).build();
            TransferManager xfer_mgr = TransferManagerBuilder.standard().withS3Client(s3Client).build();

            try {
                Upload xfer = xfer_mgr.upload(BUCKET_NAME,  Authentification.getInstance().getUser().getId() + "/" + fileName, f);
                xfer.addProgressListener((ProgressListener) progressEvent -> {
                    ProgressEventType progressEventType = progressEvent.getEventType();
                    if (progressEventType == ProgressEventType.TRANSFER_COMPLETED_EVENT) {
                        addCourseToDatabase(signedFile.getUrl());
                    }
                    else if (progressEventType == ProgressEventType.TRANSFER_FAILED_EVENT || progressEventType == ProgressEventType.CLIENT_REQUEST_FAILED_EVENT) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                progressLabel.setText("Erreur lors de l'upload du fichier");
                            }
                        });
                    }
                });
            } catch (AmazonServiceException e) {
                e.printStackTrace();
            } catch (SdkClientException e) {
                e.printStackTrace();
            }

        }
    }

    public void addCourseToDatabase(String fileUrl) {

        User currentUser = Authentification.getInstance().getUser();
        String courseName = fileNameTextField.getText();

        CourseCredentials courseCredentials = new CourseCredentials(courseName, this.idTopic, false, null, fileUrl, currentUser.getClasse().getId(), currentUser.getId());
        ESGIPocketProviderPost esgiPocketProviderPost = new ESGIPocketProviderPost(Authentification.getInstance().getToken());

        esgiPocketProviderPost.postCourse(courseCredentials, new ApiListener<Course>() {
            @Override
            public void onSuccess(Course response) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        progressLabel.setText("Upload termine");
                        fileNameTextField.setText("");
                        filePathTextField.setText("");
                        chooseFileButton.setDisable(false);
                    }
                });

            }

            @Override
            public void onError(Throwable throwable) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        progressLabel.setText("Erreur lors de l'upload du fichier");
                        chooseFileButton.setDisable(false);
                        uploadButton.setDisable(false);
                    }
                });
            }
        });
    }
}
