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

import com.io7m.jxtrand.api.JXTStringsType;
import com.io7m.volcanolab.services.api.VLServiceType;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * The main string resources.
 */

public final class VLMainStrings
  implements VLServiceType, JXTStringsType
{
  private final ResourceBundle resources;

  /**
   * The main string resources.
   *
   * @param locale The application locale
   */

  public VLMainStrings(
    final Locale locale)
  {
    this.resources =
      ResourceBundle.getBundle(
        "/com/io7m/volcanolab/gui/internal/Messages",
        locale
      );
  }

  @Override
  public ResourceBundle resources()
  {
    return this.resources;
  }

  @Override
  public String toString()
  {
    return String.format(
      "[VLMainStrings 0x%08x]",
      Integer.valueOf(this.hashCode())
    );
  }
}
