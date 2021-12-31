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

package com.io7m.volcanolab.tests;

import com.io7m.jcoronado.api.VulkanPhysicalDeviceType;
import com.io7m.volcanolab.experiment.api.ExperimentContextType;

import java.util.Objects;

public final class ExperimentTestContext implements ExperimentContextType
{
  private final VulkanPhysicalDeviceType physicalDevice;
  private int width;
  private int height;

  public ExperimentTestContext(
    final VulkanPhysicalDeviceType inPhysicalDevice)
  {
    this.physicalDevice =
      Objects.requireNonNull(inPhysicalDevice, "physicalDevice");
    this.width = 600;
    this.height = 400;
  }

  @Override
  public VulkanPhysicalDeviceType physicalDevice()
  {
    return this.physicalDevice;
  }

  public void setWidth(
    final int newWidth)
  {
    this.width = newWidth;
  }

  public void setHeight(
    final int newHeight)
  {
    this.height = newHeight;
  }

  @Override
  public int width()
  {
    return this.width;
  }

  @Override
  public int height()
  {
    return this.height;
  }
}
