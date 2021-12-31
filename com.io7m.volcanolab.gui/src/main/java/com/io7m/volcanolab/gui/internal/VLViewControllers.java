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


import com.io7m.volcanolab.services.api.VLServiceDirectoryType;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * A directory of view controllers.
 */

public final class VLViewControllers
{
  private static final Logger LOG =
    LoggerFactory.getLogger(VLViewControllers.class);

  private VLViewControllers()
  {

  }

  /**
   * Create a view controller.
   *
   * @param clazz        The view controller class
   * @param stage        The stage
   * @param mainServices The service directory
   *
   * @return A view controller
   */

  public static Object createController(
    final Class<?> clazz,
    final Stage stage,
    final VLServiceDirectoryType mainServices)
  {
    LOG.debug("createController: {}", clazz);

    if (Objects.equals(clazz, VLViewControllerMain.class)) {
      return new VLViewControllerMain(mainServices, stage);
    }
    if (Objects.equals(clazz, VLViewControllerDevices.class)) {
      return new VLViewControllerDevices(mainServices, stage);
    }

    throw new IllegalStateException(
      String.format("Unrecognized class: %s", clazz)
    );
  }
}
