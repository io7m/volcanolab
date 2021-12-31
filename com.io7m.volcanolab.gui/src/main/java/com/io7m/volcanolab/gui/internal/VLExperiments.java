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

import com.io7m.jcoronado.api.VulkanApplicationInfo;
import com.io7m.jcoronado.api.VulkanException;
import com.io7m.jcoronado.api.VulkanInstanceCreateInfo;
import com.io7m.jcoronado.api.VulkanInstanceProviderType;
import com.io7m.jcoronado.api.VulkanInstanceType;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceIDProperties;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceType;
import com.io7m.jcoronado.api.VulkanUncheckedException;
import com.io7m.jcoronado.api.VulkanVersions;
import com.io7m.jcoronado.extensions.ext_debug_utils.api.VulkanDebugUtilsMessageSeverityFlag;
import com.io7m.jcoronado.extensions.ext_debug_utils.api.VulkanDebugUtilsMessageTypeFlag;
import com.io7m.jcoronado.extensions.ext_debug_utils.api.VulkanDebugUtilsMessengerCreateInfoEXT;
import com.io7m.jcoronado.extensions.ext_debug_utils.api.VulkanDebugUtilsSLF4J;
import com.io7m.jcoronado.extensions.ext_debug_utils.api.VulkanDebugUtilsType;
import com.io7m.jcoronado.lwjgl.VulkanLWJGLInstanceProvider;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import com.io7m.volcanolab.experiment.api.ExperimentContextType;
import com.io7m.volcanolab.experiment.api.ExperimentEventType;
import com.io7m.volcanolab.experiment.api.ExperimentType;
import com.io7m.volcanolab.gui.internal.VLExperimentEventType.VLExperimentEvent;
import com.io7m.volcanolab.gui.internal.VLExperimentEventType.VLExperimentSelected;
import com.io7m.volcanolab.preferences.api.VLPreferences;
import com.io7m.volcanolab.preferences.api.VLPreferencesDeviceSelection;
import com.io7m.volcanolab.preferences.api.VLPreferencesServiceType;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.io7m.volcanolab.gui.internal.VLExperimentEventType.VLExperimentSizeChanged;

public final class VLExperiments implements VLExperimentsServiceType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(VLExperiments.class);

  private final ExecutorService executor;
  private final VulkanInstanceProviderType instances;
  private final TreeMap<String, ExperimentType> experiments;
  private final VLPreferencesServiceType preferences;
  private final LinkedBlockingQueue<WrappedCommand<?>> commands;
  private final AtomicBoolean stopped;
  private final AtomicReference<VulkanPhysicalDeviceType> device;
  private final AtomicReference<ExperimentType> experiment;
  private final AtomicReference<ImageContext> imageContext;
  private final PublishSubject<VLExperimentEventType> events;
  private final CloseableCollectionType<ClosingResourceFailedException> resources;
  private final ExecutionContext execContext;
  private final SimpleObjectProperty<VLDeviceSelection> deviceProperty;
  private VulkanInstanceType instance;
  private final DoubleProperty frameTime;
  private volatile Disposable experimentSubscription;

  private VLExperiments(
    final ExecutorService inExecutor,
    final VulkanInstanceProviderType inInstances,
    final TreeMap<String, ExperimentType> inExperiments,
    final VLPreferencesServiceType inPreferences)
  {
    this.executor = inExecutor;
    this.instances = inInstances;
    this.experiments = inExperiments;
    this.preferences = inPreferences;
    this.commands = new LinkedBlockingQueue<>();
    this.stopped = new AtomicBoolean(false);
    this.device = new AtomicReference<>();
    this.experiment = new AtomicReference<>();
    this.imageContext = new AtomicReference<>();
    this.events = PublishSubject.create();
    this.resources = CloseableCollection.create();
    this.deviceProperty = new SimpleObjectProperty<>();
    this.frameTime = new SimpleDoubleProperty(0.0);
    this.execContext = new ExecutionContext(this);
  }

  private static final class ExecutionContext
    implements ExperimentContextType
  {
    private final VLExperiments owner;

    ExecutionContext(
      final VLExperiments inOwner)
    {
      this.owner = inOwner;
    }

    @Override
    public VulkanPhysicalDeviceType physicalDevice()
    {
      return this.owner.device.get();
    }

    @Override
    public int width()
    {
      return this.owner.imageContext.get().width();
    }

    @Override
    public int height()
    {
      return this.owner.imageContext.get().height();
    }
  }

  public static VLExperimentsServiceType create(
    final VLPreferencesServiceType preferences)
  {
    Objects.requireNonNull(preferences, "preferences");

    final var executor =
      Executors.newSingleThreadExecutor(
        runnable -> {
          final var thread = new Thread(runnable);
          thread.setName(
            String.format("com.io7m.volcanolab.render[%d]", thread.getId())
          );
          return thread;
        }
      );

    final var instances =
      VulkanLWJGLInstanceProvider.create();

    final var experiments =
      new TreeMap<>(
        ServiceLoader.load(ExperimentType.class)
          .stream()
          .map(ServiceLoader.Provider::get)
          .collect(Collectors.toMap(ExperimentType::name, Function.identity()))
      );

    LOG.debug("loaded {} experiments", experiments.size());

    final var controller =
      new VLExperiments(executor, instances, experiments, preferences);

    executor.execute(controller::process);

    preferences.preferences()
      .deviceSelection()
      .ifPresent(p -> controller.setPhysicalDevice(
        new VLDeviceSelection(p.deviceName(), p.deviceUUID()))
      );

    return controller;
  }

  private static PixelBuffer<ByteBuffer> initializeImageBuffer(
    final int width,
    final int height)
  {
    final var bufferSize =
      width * height * 4;
    final var byteBuffer =
      ByteBuffer.allocateDirect(bufferSize);

    for (var index = 0; index < bufferSize; index += 4) {
      byteBuffer.put(index + 0, (byte) 0x00);
      byteBuffer.put(index + 1, (byte) 0x00);
      byteBuffer.put(index + 2, (byte) 0x00);
      byteBuffer.put(index + 3, (byte) 0xff);
    }

    final PixelFormat<ByteBuffer> pixelFormat =
      PixelFormat.getByteBgraPreInstance();
    return new PixelBuffer<>(width, height, byteBuffer, pixelFormat);
  }

  @Override
  public Observable<VLExperimentEventType> events()
  {
    return this.events;
  }

  @Override
  public List<String> experiments()
  {
    return List.copyOf(this.experiments.keySet());
  }

  @Override
  public ReadOnlyDoubleProperty frameTimeProperty()
  {
    return this.frameTime;
  }

  @Override
  public ReadOnlyProperty<VLDeviceSelection> deviceProperty()
  {
    return this.deviceProperty;
  }

  @Override
  public CompletableFuture<Void> setScreenSize(
    final int width,
    final int height)
  {
    return this.submit(Void.class, () -> this.opSetSize(width, height));
  }

  @Override
  public CompletableFuture<Void> setPhysicalDevice(
    final VLDeviceSelection selection)
  {
    return this.submit(Void.class, () -> this.opSetPhysicalDevice(selection));
  }

  @Override
  public CompletableFuture<Void> setExperiment(
    final String name)
  {
    return this.submit(Void.class, () -> this.opSetExperiment(name));
  }

  @Override
  public CompletableFuture<VLDevicePropertiesList> listDevices()
  {
    return this.submit(VLDevicePropertiesList.class, this::opListDevices);
  }

  @Override
  public void close()
    throws Exception
  {
    this.stopped.compareAndSet(false, true);
  }

  private <T> CompletableFuture<T> submit(
    final Class<T> resultClass,
    final Callable<T> command)
  {
    final var future =
      new CompletableFuture<T>();
    final var wrapped =
      new WrappedCommand<T>(future, resultClass, command);

    this.commands.add(wrapped);
    return future;
  }

  private void process()
  {
    while (!this.stopped.get()) {
      try {
        final WrappedCommand<Object> command =
          (WrappedCommand<Object>) (Object) this.commands.poll();

        if (command != null) {
          try {
            final var result = command.command.call();
            final var resultT = command.resultClass.cast(result);
            command.future.complete(resultT);
          } catch (final Exception e) {
            command.future.completeExceptionally(e);
          }
        }

        this.processRender();
      } catch (final Exception e) {
        LOG.error("process: ", e);
      }
    }

    try {
      this.resources.close();
    } catch (final ClosingResourceFailedException e) {
      LOG.error("close: ", e);
    } finally {
      this.executor.shutdown();
    }
  }

  private void processRender()
  {
    final var deviceNow = this.device.get();
    if (deviceNow == null) {
      return;
    }

    final var experimentNow = this.experiment.get();
    if (experimentNow == null) {
      return;
    }

    final var imageNow = this.imageContext.get();
    if (imageNow == null) {
      return;
    }

    try {
      final var timeThen = Instant.now();
      final var byteBuffer = imageNow.imageBuffer.getBuffer();
      experimentNow.render(this.execContext, byteBuffer);
      final var timeNow = Instant.now();

      final var timeNext = timeThen.plusMillis(16L);
      final var timeWait = Duration.between(timeNow, timeNext);
      final var timeWaitMs = timeWait.toMillis();
      if (timeWaitMs > 0L) {
        Thread.sleep(timeWaitMs);
      }

      Platform.runLater(() -> {
        this.frameTime.set(
          (double) Duration.between(timeThen, timeNow).toNanos() / 1000000.0
        );
        imageNow.imageBuffer.updateBuffer(param -> null);
      });
    } catch (final Exception e) {
      LOG.error("experiment error: ", e);
    }
  }

  private Void opSetSize(
    final int width,
    final int height)
  {
    final var imageBuffer =
      initializeImageBuffer(width, height);
    this.imageContext.set(
      new ImageContext(imageBuffer, width, height));
    this.events.onNext(
      new VLExperimentSizeChanged(width, height, imageBuffer)
    );
    return null;
  }

  private Void opSetPhysicalDevice(
    final VLDeviceSelection selection)
    throws Exception
  {
    final var foundDeviceOpt =
      this.createOrGetInstance()
        .enumeratePhysicalDevices()
        .filter(candidate -> deviceMatches(
          selection.name(),
          selection.deviceId(),
          candidate))
        .findFirst();

    if (foundDeviceOpt.isEmpty()) {
      return null;
    }

    this.device.set(foundDeviceOpt.get());

    Platform.runLater(() -> {
      this.deviceProperty.set(selection);
    });

    this.preferences.update(currentPreferences -> {
      return new VLPreferences(
        currentPreferences.debuggingEnabled(),
        Optional.of(new VLPreferencesDeviceSelection(
          selection.name(),
          selection.deviceId()))
      );
    });

    return null;
  }

  private Void opSetExperiment(
    final String name)
    throws Exception
  {
    final var experimentNow =
      this.experiment.getAndSet(null);

    if (experimentNow != null) {
      experimentNow.close();
      final var sub = this.experimentSubscription;
      if (sub != null) {
        sub.dispose();
      }
    }

    final var experimentNext = this.experiments.get(name);
    if (experimentNext == null) {
      return null;
    }

    this.resources.add(experimentNext);
    this.experimentSubscription =
      experimentNext.events()
        .subscribe(this::onExperimentEvent);

    experimentNext.start(this.execContext);
    this.experiment.set(experimentNext);
    this.events.onNext(new VLExperimentSelected(name));
    return null;
  }

  private void onExperimentEvent(
    final ExperimentEventType e)
  {
    this.events.onNext(new VLExperimentEvent(e));
  }

  private VLDevicePropertiesList opListDevices()
    throws VulkanException
  {
    return new VLDevicePropertiesList(
      this.createOrGetInstance()
        .enumeratePhysicalDevices()
        .map(VLExperiments::devicePropertiesOf)
        .collect(Collectors.toList())
    );
  }

  private static VLDeviceProperties devicePropertiesOf(
    final VulkanPhysicalDeviceType device)
  {
    try {
      return new VLDeviceProperties(
        device.properties(),
        device.idProperties(),
        device.driverProperties()
      );
    } catch (final VulkanException e) {
      throw new VulkanUncheckedException(e);
    }
  }

  private static boolean deviceMatches(
    final String deviceName,
    final Optional<UUID> deviceUUID,
    final VulkanPhysicalDeviceType device)
  {
    try {
      final var idOpt = device.idProperties();
      if (deviceUUID.isPresent() && idOpt.isPresent()) {
        return deviceIdMatches(deviceUUID.get(), idOpt.get());
      }
      final var properties = device.properties();
      return Objects.equals(properties.name(), deviceName);
    } catch (final VulkanException e) {
      throw new VulkanUncheckedException(e);
    }
  }

  private static boolean deviceIdMatches(
    final UUID uuid,
    final VulkanPhysicalDeviceIDProperties idProperties)
  {
    return Objects.equals(idProperties.deviceUUID(), uuid);
  }

  private VulkanInstanceType createOrGetInstance()
    throws VulkanException
  {
    if (this.instance != null) {
      return this.instance;
    }

    final var vulkanVersion =
      VulkanVersions.encode(this.instances.findSupportedInstanceVersion());

    final var appInfo =
      VulkanApplicationInfo.builder()
        .setApplicationName("com.io7m.volcanolab")
        .setApplicationVersion(VulkanVersions.encode(0, 0, 1))
        .setVulkanAPIVersion(vulkanVersion)
        .setEngineVersion(VulkanVersions.encode(0, 0, 1))
        .setEngineName("com.io7m.volcanolab")
        .build();

    final VulkanInstanceCreateInfo createInfo =
      VulkanInstanceCreateInfo.builder()
        .addEnabledLayers("VK_LAYER_KHRONOS_validation")
        .addEnabledExtensions("VK_EXT_debug_utils")
        .setApplicationInfo(appInfo)
        .build();

    this.instance =
      this.resources.add(
        this.instances.createInstance(createInfo, Optional.empty()));

    /*
     * Enable debug messages.
     */

    final var debug =
      this.instance.findEnabledExtension(
        "VK_EXT_debug_utils",
        VulkanDebugUtilsType.class
      ).orElseThrow(() -> {
        return new IllegalStateException(
          "Missing VK_EXT_debug_utils extension");
      });

    this.resources.add(
      debug.createDebugUtilsMessenger(
        this.instance,
        VulkanDebugUtilsMessengerCreateInfoEXT.builder()
          .setSeverity(EnumSet.allOf(VulkanDebugUtilsMessageSeverityFlag.class))
          .setType(EnumSet.allOf(VulkanDebugUtilsMessageTypeFlag.class))
          .setCallback(new VulkanDebugUtilsSLF4J(LOG))
          .build()
      )
    );
    return this.instance;
  }

  private record ImageContext(
    PixelBuffer<ByteBuffer> imageBuffer,
    int width,
    int height)
  {

  }

  private record WrappedCommand<T>(
    CompletableFuture<T> future,
    Class<T> resultClass,
    Callable<T> command)
  {

  }

  @Override
  public String toString()
  {
    return String.format(
      "[VLExperiments 0x%08x]",
      Integer.valueOf(this.hashCode())
    );
  }
}
