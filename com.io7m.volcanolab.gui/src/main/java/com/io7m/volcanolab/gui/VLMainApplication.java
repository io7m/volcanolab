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

package com.io7m.volcanolab.gui;

import com.io7m.volcanolab.gui.internal.VLMainServices;
import com.io7m.volcanolab.gui.internal.VLMainStrings;
import com.io7m.volcanolab.gui.internal.VLViewControllerMain;
import com.io7m.volcanolab.gui.internal.VLViewControllers;
import com.io7m.volcanolab.services.api.VLServiceDirectoryType;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * The main application entry point.
 */

public final class VLMainApplication extends Application
{
  private static final Logger LOG =
    LoggerFactory.getLogger(VLMainApplication.class);

  private VLServiceDirectoryType mainServices;

  /**
   * The main application entry point.
   */

  public VLMainApplication()
  {

  }

  @Override
  public void start(final Stage stage)
    throws Exception
  {
    LOG.debug("starting application");

    this.mainServices =
      VLMainServices.create();
    final var strings =
      this.mainServices.requireService(VLMainStrings.class);
    final var mainXML =
      VLViewControllerMain.class.getResource("mainWindow.fxml");
    final var loader =
      new FXMLLoader(mainXML, strings.resources());

    stage.setOnCloseRequest(event -> {
      try {
        this.mainServices.close();
      } catch (final IOException e) {
        LOG.error("close: ", e);
      }
    });

    loader.setControllerFactory(
      clazz -> VLViewControllers.createController(
        clazz, stage, this.mainServices));

    final AnchorPane pane = loader.load();
    final var controller = (VLViewControllerMain) loader.getController();

    stage.setTitle(strings.format("programTitle"));
    stage.setMinWidth(1024.0);
    stage.setMinHeight(768.0);
    stage.setScene(new Scene(pane));
    stage.show();
  }
}
