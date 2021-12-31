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

package com.io7m.volcanolab.preferences.vanilla.internal;

import com.io7m.volcanolab.preferences.api.VLPreferences;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Properties;

import static com.io7m.volcanolab.preferences.vanilla.internal.VLPreferencesKeys.DEBUGGING;
import static com.io7m.volcanolab.preferences.vanilla.internal.VLPreferencesKeys.DEVICE_NAME;
import static com.io7m.volcanolab.preferences.vanilla.internal.VLPreferencesKeys.DEVICE_UUID;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A storer of preferences.
 */

public final class VLPreferencesStorer
{
  private final OutputStream stream;
  private final VLPreferences preferences;
  private Properties properties;

  /**
   * A storer of preferences.
   *
   * @param inStream      The output stream
   * @param inPreferences The preferences
   */

  public VLPreferencesStorer(
    final OutputStream inStream,
    final VLPreferences inPreferences)
  {
    this.stream =
      Objects.requireNonNull(inStream, "stream");
    this.preferences =
      Objects.requireNonNull(inPreferences, "preferences");
  }

  /**
   * Store preferences.
   *
   * @throws IOException On I/O errors
   */

  public void store()
    throws IOException
  {
    this.properties = new Properties();
    this.storeDebugging();
    this.storeDeviceSelection();
    this.properties.storeToXML(this.stream, "", UTF_8);
  }

  private void storeDeviceSelection()
  {
    this.preferences.deviceSelection().ifPresentOrElse(deviceSelection -> {
      this.properties.put(DEVICE_NAME, deviceSelection.deviceName());
      deviceSelection.deviceUUID().ifPresentOrElse(uuid -> {
        this.properties.put(DEVICE_UUID, uuid.toString());
      }, () -> {
        this.properties.remove(DEVICE_UUID);
      });
    }, () -> {
      this.properties.remove(DEVICE_NAME);
      this.properties.remove(DEVICE_UUID);
    });
  }

  private void storeDebugging()
  {
    this.properties.put(
      DEBUGGING,
      switch (this.preferences.debuggingEnabled()) {
        case DEBUGGING_DISABLED -> "false";
        case DEBUGGING_ENABLED -> "true";
      }
    );
  }
}
