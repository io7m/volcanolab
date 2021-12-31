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
import com.io7m.volcanolab.gui.internal.VLExperimentEventType.VLExperimentEvent;
import com.io7m.volcanolab.gui.internal.VLExperimentEventType.VLExperimentSelected;
import com.io7m.volcanolab.gui.internal.VLExperimentEventType.VLExperimentSizeChanged;
import com.io7m.volcanolab.services.api.VLServiceDirectoryType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * The main view controller.
 */

public final class VLViewControllerMain implements Initializable
{
  private final Stage stage;
  private final VLMainStrings strings;
  private final VLServiceDirectoryType services;
  private final VLExperimentsServiceType experiments;

  @FXML private Menu menuExperiments;
  @FXML private ImageView mainImage;
  @FXML private Rectangle mainImageBorder;
  @FXML private Label experimentName;
  @FXML private Label frameTime;
  @FXML private ProgressBar progressBar;
  @FXML private RadioMenuItem windowMenuFullscreen;

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
  }

  @Override
  public void initialize(
    final URL location,
    final ResourceBundle resources)
  {
    this.progressBar.setVisible(false);

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
        this.frameTime.setText(newValue + "ms");
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
}
