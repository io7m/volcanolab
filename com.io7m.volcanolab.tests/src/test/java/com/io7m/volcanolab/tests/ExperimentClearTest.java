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

import com.io7m.jcoronado.api.VulkanApplicationInfo;
import com.io7m.jcoronado.api.VulkanInstanceCreateInfo;
import com.io7m.jcoronado.api.VulkanVersions;
import com.io7m.jcoronado.lwjgl.VulkanLWJGLInstanceProvider;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.volcanolab.experiment.api.ExperimentEventType;
import com.io7m.volcanolab.experiments.ExperimentClear;
import io.reactivex.rxjava3.disposables.Disposable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public final class ExperimentClearTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(ExperimentClearTest.class);

  private CloseableCollectionType<?> resources;
  private ExperimentTestContext context;
  private ArrayList<ExperimentEventType> events;

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.events = new ArrayList<ExperimentEventType>();
    this.resources = CloseableCollection.create();

    final var instances =
      VulkanLWJGLInstanceProvider.create();

    final var applicationInfo =
      VulkanApplicationInfo.builder()
        .setVulkanAPIVersion(
          VulkanVersions.encode(instances.findSupportedInstanceVersion()))
        .setEngineName("com.io7m.volcanolab.tests")
        .setEngineVersion(VulkanVersions.encode(0, 0, 1))
        .setApplicationName("com.io7m.volcanolab.tests")
        .setApplicationVersion(VulkanVersions.encode(0, 0, 1))
        .build();

    final var createInfo =
      VulkanInstanceCreateInfo.builder()
        .setApplicationInfo(applicationInfo)
        .addEnabledLayers("VK_LAYER_KHRONOS_validation")
        .build();

    final var instance =
      this.resources.add(
        instances.createInstance(createInfo, Optional.empty())
      );

    final var physicalDevice =
      instance.physicalDevices()
        .get(0);

    this.context =
      new ExperimentTestContext(physicalDevice);
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    this.resources.close();
  }

  @Test
  public void testRun()
    throws Exception
  {
    try (var clear = new ExperimentClear()) {
      this.resources.add(
        Disposable.toAutoCloseable(clear.events().subscribe(this::onEvent))
      );
      clear.start(this.context);

      this.context.setWidth(1200);
      this.context.setHeight(800);
      clear.onSizeChanged(this.context);

      this.context.setWidth(1200);
      this.context.setHeight(800);
      clear.onSizeChanged(this.context);

      final var data = ByteBuffer.allocate(1200 * 800 * 4);
      clear.render(this.context, data);
      saveImage(1200, 800, data);
    }
  }

  private static void saveImage(
    final int width,
    final int height,
    final ByteBuffer data)
    throws IOException
  {
    Files.write(
      Paths.get("/tmp/image.data"),
      data.array(),
      CREATE,
      TRUNCATE_EXISTING,
      WRITE
    );
  }

  private void onEvent(
    final ExperimentEventType event)
  {
    LOG.debug("event: {}", event);
    this.events.add(event);
  }
}
