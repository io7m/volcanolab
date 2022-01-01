/*
 * Copyright © 2021 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.volcanolab.gui.internal;

import com.io7m.volcanolab.experiment.api.ExperimentEventLifecycle;
import com.io7m.volcanolab.experiment.api.ExperimentMouseButtons;
import com.io7m.volcanolab.gui.internal.VLExperimentEventType.VLExperimentEvent;
import com.io7m.volcanolab.gui.internal.VLExperimentEventType.VLExperimentSelected;
import com.io7m.volcanolab.gui.internal.VLExperimentEventType.VLExperimentSizeChanged;
import com.io7m.volcanolab.services.api.VLServiceDirectoryType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.robot.Robot;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.KeyEvent.KEY_RELEASED;

/**
 * The main view controller.
 */

public final class VLViewControllerMain implements Initializable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(VLViewControllerMain.class);

  private final Stage stage;
  private final VLMainStrings strings;
  private final VLServiceDirectoryType services;
  private final VLExperimentsServiceType experiments;
  private final Robot robot;

  @FXML private ImageView mainImage;
  @FXML private Label experimentName;
  @FXML private Label frameTime;
  @FXML private Menu menuExperiments;
  @FXML private ProgressBar progressBar;
  @FXML private RadioMenuItem windowMenuCaptureKeyboard;
  @FXML private RadioMenuItem windowMenuFullscreen;
  @FXML private Rectangle mainImageBorder;
  @FXML private Label captureHint;
  @FXML private Label captureKey;

  /**
   * The main view controller.
   *
   * @param mainServices The service directory
   * @param inStage      The stage
   */

  public VLViewControllerMain(
    final VLServiceDirectoryType mainServices,
    final Stage inStage)
  {
    this.stage =
      Objects.requireNonNull(inStage, "stage");
    this.services =
      Objects.requireNonNull(mainServices, "mainServices");
    this.strings =
      mainServices.requireService(VLMainStrings.class);
    this.experiments =
      mainServices.requireService(VLExperimentsServiceType.class);
    this.robot =
      new Robot();
  }

  @Override
  public void initialize(
    final URL location,
    final ResourceBundle resources)
  {
    this.progressBar.setVisible(false);

    this.stage.addEventFilter(KEY_PRESSED, this::onKeyPressed);
    this.stage.addEventFilter(KEY_RELEASED, this::onKeyReleased);
    this.stage.addEventFilter(MouseEvent.ANY, this::onMouseEvent);

    this.windowMenuCaptureKeyboard.selectedProperty()
      .addListener((observable, oldValue, newValue) -> {
        this.onWindowCaptureSelectionChanged(newValue);
      });

    this.stage.fullScreenProperty()
      .addListener((observable, oldValue, newValue) -> {
        this.windowMenuFullscreen.setSelected(newValue);
      });

    this.populateExperimentsMenu();

    this.experiments.events()
      .subscribe(this::onExperimentEvent);

    this.experiments.deviceProperty()
      .addListener((observable, oldValue, newValue) -> {
        this.onDeviceSelectionChanged(newValue);
      });

    this.experiments.frameTimeProperty()
      .addListener((observable, oldValue, newValue) -> {
        this.frameTime.setText(
          String.format(
            "%.3f ms | %f fps",
            newValue,
            1000.0 / newValue.doubleValue()));
      });

    this.experiments.setScreenSize(
      (int) this.mainImageBorder.getWidth(),
      (int) this.mainImageBorder.getHeight()
    );
  }

  private void onExperimentEvent(
    final VLExperimentEventType event)
  {
    if (event instanceof VLExperimentSelected selected) {
      Platform.runLater(() -> {
        this.experimentName.setText(selected.name());

        for (final var item : this.menuExperiments.getItems()) {
          if (item instanceof RadioMenuItem radio) {
            radio.setSelected(Objects.equals(radio.getText(), selected.name()));
          }
        }
      });
      return;
    }

    if (event instanceof VLExperimentSizeChanged sizeChanged) {
      Platform.runLater(() -> {
        this.mainImage.setImage(new WritableImage(sizeChanged.imageBuffer()));
      });
      return;
    }

    if (event instanceof VLExperimentEvent exEvent) {
      if (exEvent.event() instanceof ExperimentEventLifecycle lifecycle) {
        Platform.runLater(() -> {
          switch (lifecycle.status()) {
            case INITIALIZED, STARTED, RUNNING, STOPPED -> {
              this.progressBar.setVisible(false);
            }
            case LOADING -> {
              this.progressBar.setVisible(true);
              this.progressBar.setProgress(lifecycle.progress());
            }
          }
        });
      }
    }
  }

  private void populateExperimentsMenu()
  {
    final var names =
      this.experiments.experiments();
    final var items =
      this.menuExperiments.getItems();

    for (final var name : names) {
      final var menuItem = new RadioMenuItem(name);
      menuItem.setOnAction(event -> {
        this.experiments.setExperiment(name);
      });
      items.add(menuItem);
    }
  }

  private void onDeviceSelectionChanged(
    final VLDeviceSelection device)
  {
    if (device != null) {
      this.menuExperiments.setDisable(false);
    } else {
      this.menuExperiments.setDisable(true);
    }
  }

  private void onWindowCaptureSelectionChanged(
    final Boolean enabled)
  {
    if (enabled) {
      this.captureHint.setText(
        this.strings.format("window.capture_hint_disable"));
      return;
    }

    this.captureHint.setText(
      this.strings.format("window.capture_hint_enable"));
    this.experiments.setKeysAllReleased();
  }

  @FXML
  private void onDevicesSelected()
    throws IOException
  {
    final var stage = new Stage();

    final var connectXML =
      VLViewControllerMain.class.getResource("devices.fxml");

    final var resources =
      this.strings.resources();
    final var loader =
      new FXMLLoader(connectXML, resources);

    loader.setControllerFactory(
      clazz -> VLViewControllers.createController(clazz, stage, this.services)
    );

    final Pane pane = loader.load();
    stage.initModality(Modality.APPLICATION_MODAL);
    stage.setScene(new Scene(pane));
    stage.setTitle(this.strings.format("devices.title"));
    stage.showAndWait();
  }

  @FXML
  private void onQuitSelected()
  {
    try {
      this.services.close();
    } catch (final IOException e) {
      // Don't care
    }

    Platform.exit();
  }

  @FXML
  private void onFullScreenSelected()
  {
    this.stage.setFullScreen(this.windowMenuFullscreen.isSelected());
  }

  private void onMouseEvent(
    final MouseEvent event)
  {
    this.stage.getScene()
      .setCursor(Cursor.DEFAULT);

    if (!this.windowMenuCaptureKeyboard.isSelected()) {
      return;
    }

    this.stage.getScene()
      .setCursor(Cursor.DISAPPEAR);

    /*
     * If mouse capture is enabled, warp the cursor to the center of the
     * rendered image on every mouse event.
     */

    final var screen =
      this.mainImage.localToScreen(
        0.0,
        0.0);

    this.robot.mouseMove(screen.getX(), screen.getY());
    this.experiments.setMouseButtons(
      new ExperimentMouseButtons(
        event.isPrimaryButtonDown(),
        event.isSecondaryButtonDown()
      )
    );

    event.consume();
  }

  private void onKeyPressed(
    final KeyEvent event)
  {
    this.captureKey.setText(event.getCode().getName());

    if (event.isControlDown() && event.getCode() == KeyCode.K) {
      return;
    }

    if (!this.windowMenuCaptureKeyboard.isSelected()) {
      return;
    }

    this.experiments.setKeyDown(event.getCode());
    event.consume();
  }

  private void onKeyReleased(
    final KeyEvent event)
  {
    this.captureKey.setText(event.getCode().getName());

    if (event.isControlDown() && event.getCode() == KeyCode.K) {
      return;
    }

    if (!this.windowMenuCaptureKeyboard.isSelected()) {
      return;
    }

    this.experiments.setKeyUp(event.getCode());
    event.consume();
  }
}
