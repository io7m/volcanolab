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

import com.io7m.jproperties.JPropertyIncorrectType;
import com.io7m.volcanolab.preferences.api.VLPreferences;
import com.io7m.volcanolab.preferences.api.VLPreferencesDebuggingEnabled;
import com.io7m.volcanolab.preferences.api.VLPreferencesDeviceSelection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.FileSystem;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static com.io7m.jproperties.JProperties.getBooleanWithDefault;
import static com.io7m.jproperties.JProperties.getStringWithDefault;
import static com.io7m.jproperties.JProperties.getUUIDWithDefault;
import static com.io7m.volcanolab.preferences.api.VLPreferencesDebuggingEnabled.DEBUGGING_DISABLED;
import static com.io7m.volcanolab.preferences.api.VLPreferencesDebuggingEnabled.DEBUGGING_ENABLED;
import static com.io7m.volcanolab.preferences.vanilla.internal.VLPreferencesKeys.DEVICE_NAME;
import static com.io7m.volcanolab.preferences.vanilla.internal.VLPreferencesKeys.DEVICE_UUID;

/**
 * A preferences loader.
 */

public final class VLPreferencesLoader
{
  private static final Logger LOG =
    LoggerFactory.getLogger(VLPreferencesLoader.class);

  private final FileSystem fileSystem;
  private final Properties properties;

  /**
   * A preferences loader.
   *
   * @param inFileSystem The filesystem used for paths
   * @param inProperties Properties
   */

  public VLPreferencesLoader(
    final FileSystem inFileSystem,
    final Properties inProperties)
  {
    this.fileSystem =
      Objects.requireNonNull(inFileSystem, "fileSystem");
    this.properties =
      Objects.requireNonNull(inProperties, "properties");
  }

  /**
   * @return A loaded set of preferences
   */

  public VLPreferences load()
  {
    return new VLPreferences(
      this.loadDebuggingEnabled(),
      this.loadDeviceSelection()
    );
  }

  private Optional<VLPreferencesDeviceSelection> loadDeviceSelection()
  {
    try {
      final var deviceName =
        getStringWithDefault(this.properties, DEVICE_NAME, "");

      if (deviceName.isEmpty()) {
        return Optional.empty();
      }

      final var nullUUID =
        UUID.fromString("00000000-0000-0000-0000-000000000000");
      final var deviceUUID =
        getUUIDWithDefault(
          this.properties, DEVICE_UUID, nullUUID);

      if (deviceUUID.equals(nullUUID)) {
        return Optional.of(
          new VLPreferencesDeviceSelection(deviceName, Optional.empty()));
      }
      return Optional.of(
        new VLPreferencesDeviceSelection(deviceName, Optional.of(deviceUUID)));
    } catch (final JPropertyIncorrectType e) {
      return Optional.empty();
    }
  }

  private VLPreferencesDebuggingEnabled loadDebuggingEnabled()
  {
    try {
      if (getBooleanWithDefault(this.properties, "debugging", false)) {
        return DEBUGGING_ENABLED;
      }
      return DEBUGGING_DISABLED;
    } catch (final JPropertyIncorrectType e) {
      return DEBUGGING_DISABLED;
    }
  }
}
