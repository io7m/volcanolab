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

import com.io7m.jade.api.ApplicationDirectories;
import com.io7m.jade.api.ApplicationDirectoriesType;
import com.io7m.jade.api.ApplicationDirectoryConfiguration;
import com.io7m.volcanolab.preferences.api.VLPreferences;
import com.io7m.volcanolab.preferences.api.VLPreferencesServiceType;
import com.io7m.volcanolab.preferences.vanilla.VLPreferencesService;
import com.io7m.volcanolab.services.api.VLServiceDirectory;
import com.io7m.volcanolab.services.api.VLServiceDirectoryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;

/**
 * The main service directory.
 */

public final class VLMainServices
{
  private static final Logger LOG =
    LoggerFactory.getLogger(VLMainServices.class);

  private VLMainServices()
  {

  }

  /**
   * Create a new service directory.
   *
   * @return A service directory
   *
   * @throws IOException On I/O errors
   */

  public static VLServiceDirectoryType create()
    throws IOException
  {
    final ApplicationDirectoriesType directories =
      applicationDirectories();

    final var locale = Locale.getDefault();
    final var services = new VLServiceDirectory();
    final var mainStrings = new VLMainStrings(locale);
    final var preferences =
      VLPreferencesService.openOrDefault(
        directories.configurationDirectory()
          .resolve("volcanolab.conf"));

    services.register(VLPreferencesServiceType.class, preferences);
    services.register(VLMainStrings.class, mainStrings);
    services.register(VLExperimentsServiceType.class, VLExperiments.create(preferences));
    return services;
  }

  private static ApplicationDirectoriesType applicationDirectories()
  {
    final var configuration =
      ApplicationDirectoryConfiguration.builder()
        .setApplicationName("com.io7m.volcanolab")
        .setPortablePropertyName("com.io7m.volcanolab.portable")
        .build();

    return ApplicationDirectories.get(configuration);
  }
}
