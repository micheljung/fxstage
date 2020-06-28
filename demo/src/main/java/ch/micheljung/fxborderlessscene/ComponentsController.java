package ch.micheljung.fxborderlessscene;

import ch.micheljung.fxwindow.FxStage;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Optional;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class ComponentsController {
  public AnchorPane root;
  public TextField fileTextField;
  public TableView<Item> table;
  public TableColumn<Item, String> idColumn;
  public TableColumn<Item, String> nameColumn;
  private WatchService watchService;

  public void initialize() {
    table.getItems().setAll(
      new Item("1", "John Doe"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster"),
      new Item("2", "Max Muster")
    );
    idColumn.setCellValueFactory(param -> param.getValue().id);
    nameColumn.setCellValueFactory(param -> param.getValue().name);
  }

  public void chooseFile() {
    Window window = root.getScene().getWindow();

    FileChooser fileChooser = new FileChooser();
    Optional.ofNullable(fileChooser.showOpenDialog(window))
      .map(File::toPath)
      .ifPresent(this::onFileChosen);
  }

  private void onFileChosen(Path file) {
    if (!Files.isRegularFile(file)) {
      return;
    }
    try {
      watchFile(file);
      loadStyleSheet(file);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private boolean loadStyleSheet(Path file) {
    try {
      return root.getScene().getStylesheets().setAll(
        FxStage.BASE_CSS.toExternalForm(),
        FxStage.UNDECORATED_CSS.toExternalForm(),
        file.toUri().toURL().toExternalForm()
      );
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  private void watchFile(Path file) throws Exception {
    if (watchService != null) {
      watchService.close();
    }
    watchService = file.getFileSystem().newWatchService();
    Thread watchThread = new Thread(() -> {
      try {
        while (!Thread.interrupted()) {
          WatchKey key = watchService.take();
          for (WatchEvent<?> watchEvent : key.pollEvents()) {
            Path path = (Path) watchEvent.context();
            if (file.getFileName().equals(path)) {
              Platform.runLater(() -> loadStyleSheet(file));
            }
          }
          key.reset();
        }
      } catch (InterruptedException | ClosedWatchServiceException e) {
        // Terminated
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    watchThread.setDaemon(true);
    watchThread.start();

    file.getParent().register(watchService, ENTRY_CREATE, ENTRY_MODIFY);
  }

  public void onFileChosen(ActionEvent actionEvent) {
    onFileChosen(Paths.get(fileTextField.getText()));
  }

  private static class Item {
    private final StringProperty id;
    private final StringProperty name;

    public Item(String id, String name) {
      this.id = new SimpleStringProperty(id);
      this.name = new SimpleStringProperty(name);
    }
  }
}
