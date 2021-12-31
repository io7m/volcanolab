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

import com.io7m.jcoronado.api.VulkanPhysicalDeviceIDProperties;
import com.io7m.jcoronado.api.VulkanVendorIDs;
import com.io7m.volcanolab.preferences.api.VLPreferencesDeviceSelection;
import com.io7m.volcanolab.preferences.api.VLPreferencesServiceType;
import com.io7m.volcanolab.services.api.VLServiceDirectoryType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;

/**
 * The devices view controller.
 */

public final class VLViewControllerDevices implements Initializable
{
  private final Stage stage;
  private final VLExperimentsServiceType experiments;
  private final VLPreferencesServiceType preferences;

  @FXML
  private ListView<VLDeviceProperties> deviceList;
  @FXML
  private TextField deviceId;
  @FXML
  private TextField deviceVendor;
  @FXML
  private TextField deviceName;
  @FXML
  private TextField deviceType;
  @FXML
  private Button selectButton;
  @FXML
  private Button cancelButton;
  @FXML
  private ProgressIndicator progressIndicator;
  @FXML
  private TextField deviceDriverId;
  @FXML
  private TextField deviceDriverName;
  @FXML
  private TextField deviceDriverInfo;

  /**
   * The devices view controller.
   *
   * @param mainServices The service directory
   * @param inStage      The stage
   */

  public VLViewControllerDevices(
    final VLServiceDirectoryType mainServices,
    final Stage inStage)
  {
    Objects.requireNonNull(mainServices, "mainServices");

    this.stage =
      Objects.requireNonNull(inStage, "stage");
    this.experiments =
      mainServices.requireService(VLExperimentsServiceType.class);
    this.preferences =
      mainServices.requireService(VLPreferencesServiceType.class);
  }

  @Override
  public void initialize(
    final URL location,
    final ResourceBundle resources)
  {
    this.progressIndicator.setVisible(true);
    this.deviceList.setDisable(true);
    this.deviceList.setCellFactory(param -> new VLDeviceListItemCell());
    this.deviceList.getSelectionModel()
      .selectedItemProperty()
      .addListener((observable, oldValue, newValue) ->
                     this.onDeviceSelected(newValue));

    final var devicesFuture =
      this.experiments.listDevices();

    devicesFuture.thenRun(() -> {
      Platform.runLater(() -> {
        try {
          this.onDevicesReceived(devicesFuture.get());
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        } catch (final ExecutionException e) {
          final var cause = e.getCause();
          this.progressIndicator.setVisible(false);
          this.deviceList.setDisable(true);
        }
      });
    });
  }

  private void onDeviceSelected(
    final VLDeviceProperties selectedDevice)
  {
    if (selectedDevice == null) {
      this.selectButton.setDisable(true);
      this.deviceName.setText("");
      this.deviceType.setText("");
      this.deviceVendor.setText("");
      this.deviceDriverId.setText("");
      this.deviceDriverName.setText("");
      this.deviceDriverInfo.setText("");
      this.deviceId.setText("");
      return;
    }

    final var deviceProperties =
      selectedDevice.properties();

    this.selectButton.setDisable(false);
    this.deviceName.setText(
      deviceProperties.name());
    this.deviceVendor.setText(
      VulkanVendorIDs.vendorName(deviceProperties.vendorId()));
    this.deviceType.setText(
      deviceProperties.type().toString()
    );

    selectedDevice.driverProperties().ifPresent(driver -> {
      this.deviceDriverId.setText(driver.driverId().toString());
      this.deviceDriverInfo.setText(driver.driverInfo());
      this.deviceDriverName.setText(driver.driverName());
    });

    selectedDevice.idProperties().ifPresent(id -> {
      this.deviceId.setText(id.deviceUUID().toString());
    });
  }

  private void onDevicesReceived(
    final VLDevicePropertiesList devices)
  {
    this.deviceList.getItems().setAll(devices.devices());
    this.progressIndicator.setVisible(false);
    this.deviceList.setDisable(false);

    this.preferences.preferences()
      .deviceSelection()
      .ifPresent(this::selectFromPreferences);
  }

  private void selectFromPreferences(
    final VLPreferencesDeviceSelection deviceSelection)
  {
    this.deviceList.getItems()
      .stream()
      .filter(d -> deviceMatches(d, deviceSelection))
      .findFirst()
      .ifPresent(deviceProperties -> {
        this.deviceList.getSelectionModel()
          .select(deviceProperties);
      });
  }

  private static boolean deviceMatches(
    final VLDeviceProperties deviceProperties,
    final VLPreferencesDeviceSelection deviceSelection)
  {
    final var nameMatches =
      Objects.equals(
        deviceProperties.properties().name(), deviceSelection.deviceName());

    final var idMatches =
      deviceProperties.idProperties()
        .flatMap(idProperties -> {
          return deviceSelection.deviceUUID().flatMap(id -> {
            if (Objects.equals(idProperties.deviceUUID(), id)) {
              return Optional.of(true);
            }
            return Optional.of(false);
          });
        }).orElse(false);

    return nameMatches && idMatches;
  }

  @FXML
  private void onCancelSelected()
  {
    this.stage.close();
  }

  @FXML
  private void onSelectSelected()
  {
    final var device =
      this.deviceList.getSelectionModel()
        .getSelectedItem();

    this.experiments.setPhysicalDevice(
      new VLDeviceSelection(
        device.properties().name(),
        device.idProperties().map(VulkanPhysicalDeviceIDProperties::deviceUUID)
      )
    );
    this.stage.close();
  }
}
