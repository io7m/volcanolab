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

package com.io7m.volcanolab.experiments;

import com.io7m.jcoronado.api.VulkanAttachmentDescription;
import com.io7m.jcoronado.api.VulkanAttachmentReference;
import com.io7m.jcoronado.api.VulkanBufferCreateInfo;
import com.io7m.jcoronado.api.VulkanBufferImageCopy;
import com.io7m.jcoronado.api.VulkanClearAttachment;
import com.io7m.jcoronado.api.VulkanClearRectangle;
import com.io7m.jcoronado.api.VulkanClearValueColorFloatingPoint;
import com.io7m.jcoronado.api.VulkanCommandBufferType;
import com.io7m.jcoronado.api.VulkanCommandPoolCreateInfo;
import com.io7m.jcoronado.api.VulkanComponentMappingType;
import com.io7m.jcoronado.api.VulkanException;
import com.io7m.jcoronado.api.VulkanExtent2D;
import com.io7m.jcoronado.api.VulkanExtent3D;
import com.io7m.jcoronado.api.VulkanFenceCreateInfo;
import com.io7m.jcoronado.api.VulkanFenceType;
import com.io7m.jcoronado.api.VulkanFramebufferCreateInfo;
import com.io7m.jcoronado.api.VulkanImageAspectFlag;
import com.io7m.jcoronado.api.VulkanImageCreateInfo;
import com.io7m.jcoronado.api.VulkanImageSubresourceLayers;
import com.io7m.jcoronado.api.VulkanImageSubresourceRange;
import com.io7m.jcoronado.api.VulkanImageViewCreateInfo;
import com.io7m.jcoronado.api.VulkanImageViewKind;
import com.io7m.jcoronado.api.VulkanLogicalDeviceCreateInfo;
import com.io7m.jcoronado.api.VulkanLogicalDeviceQueueCreateInfo;
import com.io7m.jcoronado.api.VulkanLogicalDeviceType;
import com.io7m.jcoronado.api.VulkanMappedMemoryType;
import com.io7m.jcoronado.api.VulkanOffset2D;
import com.io7m.jcoronado.api.VulkanOffset3D;
import com.io7m.jcoronado.api.VulkanQueueType;
import com.io7m.jcoronado.api.VulkanRectangle2D;
import com.io7m.jcoronado.api.VulkanRenderPassBeginInfo;
import com.io7m.jcoronado.api.VulkanRenderPassCreateInfo;
import com.io7m.jcoronado.api.VulkanSubmitInfo;
import com.io7m.jcoronado.api.VulkanSubpassDescription;
import com.io7m.jcoronado.lwjgl.VMALWJGLAllocatorProvider;
import com.io7m.jcoronado.vma.VMAAllocationCreateInfo;
import com.io7m.jcoronado.vma.VMAAllocatorCreateInfo;
import com.io7m.jcoronado.vma.VMAAllocatorType;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import com.io7m.volcanolab.experiment.api.ExperimentContextType;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.io7m.jcoronado.api.VulkanAttachmentLoadOp.VK_ATTACHMENT_LOAD_OP_CLEAR;
import static com.io7m.jcoronado.api.VulkanAttachmentLoadOp.VK_ATTACHMENT_LOAD_OP_DONT_CARE;
import static com.io7m.jcoronado.api.VulkanAttachmentStoreOp.VK_ATTACHMENT_STORE_OP_DONT_CARE;
import static com.io7m.jcoronado.api.VulkanAttachmentStoreOp.VK_ATTACHMENT_STORE_OP_STORE;
import static com.io7m.jcoronado.api.VulkanBufferUsageFlag.VK_BUFFER_USAGE_TRANSFER_DST_BIT;
import static com.io7m.jcoronado.api.VulkanCommandBufferLevel.VK_COMMAND_BUFFER_LEVEL_PRIMARY;
import static com.io7m.jcoronado.api.VulkanFormat.VK_FORMAT_A8B8G8R8_UNORM_PACK32;
import static com.io7m.jcoronado.api.VulkanImageAspectFlag.VK_IMAGE_ASPECT_COLOR_BIT;
import static com.io7m.jcoronado.api.VulkanImageKind.VK_IMAGE_TYPE_2D;
import static com.io7m.jcoronado.api.VulkanImageLayout.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
import static com.io7m.jcoronado.api.VulkanImageLayout.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL;
import static com.io7m.jcoronado.api.VulkanImageLayout.VK_IMAGE_LAYOUT_UNDEFINED;
import static com.io7m.jcoronado.api.VulkanImageTiling.VK_IMAGE_TILING_OPTIMAL;
import static com.io7m.jcoronado.api.VulkanImageUsageFlag.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
import static com.io7m.jcoronado.api.VulkanImageUsageFlag.VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
import static com.io7m.jcoronado.api.VulkanMemoryPropertyFlag.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
import static com.io7m.jcoronado.api.VulkanMemoryPropertyFlag.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;
import static com.io7m.jcoronado.api.VulkanPipelineBindPoint.VK_PIPELINE_BIND_POINT_GRAPHICS;
import static com.io7m.jcoronado.api.VulkanQueueFamilyPropertyFlag.VK_QUEUE_GRAPHICS_BIT;
import static com.io7m.jcoronado.api.VulkanSampleCountFlag.VK_SAMPLE_COUNT_1_BIT;
import static com.io7m.jcoronado.api.VulkanSharingMode.VK_SHARING_MODE_EXCLUSIVE;
import static com.io7m.jcoronado.api.VulkanSubpassContents.VK_SUBPASS_CONTENTS_INLINE;
import static com.io7m.jcoronado.vma.VMAAllocationCreateFlag.VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT;
import static com.io7m.jcoronado.vma.VMAMemoryUsage.VMA_MEMORY_USAGE_GPU_ONLY;
import static com.io7m.jcoronado.vma.VMAMemoryUsage.VMA_MEMORY_USAGE_GPU_TO_CPU;
import static com.io7m.volcanolab.experiment.api.ExperimentLifecycleStatus.INITIALIZED;
import static com.io7m.volcanolab.experiment.api.ExperimentLifecycleStatus.LOADING;
import static com.io7m.volcanolab.experiment.api.ExperimentLifecycleStatus.RUNNING;
import static com.io7m.volcanolab.experiment.api.ExperimentLifecycleStatus.STARTED;

public final class ExperimentClear extends ExperimentAbstract
{
  private CloseableCollectionType<ClosingResourceFailedException> frameResources;
  private VulkanLogicalDeviceType device;
  private VMAAllocatorType vmaAllocator;
  private VulkanQueueType queue;
  private VulkanCommandBufferType commandBuffer;
  private VulkanMappedMemoryType mappedFramebuffer;
  private VulkanFenceType renderFence;

  public ExperimentClear()
  {
    super(LoggerFactory.getLogger(ExperimentClear.class), "Clear");
  }

  @Override
  protected void closeActual()
  {
    try {
      this.device.waitIdle();
    } catch (final VulkanException e) {
      // Nothing we can do about it
    }
  }

  @Override
  protected void startActual(
    final ExperimentContextType context)
    throws Exception
  {
    this.eventLifecycle(INITIALIZED, 0.0, "");
    this.eventLifecycle(LOADING, 0.0, "");

    final var physicalDevice =
      context.physicalDevice();

    final var queue =
      physicalDevice.queueFamilyFindWithFlags(VK_QUEUE_GRAPHICS_BIT)
        .orElseThrow();

    final var deviceInfoBuilder =
      VulkanLogicalDeviceCreateInfo.builder();

    deviceInfoBuilder.addQueueCreateInfos(
      VulkanLogicalDeviceQueueCreateInfo.builder()
        .setQueueCount(1)
        .setQueueFamilyIndex(queue.queueFamilyIndex())
        .setQueuePriorities(1.0f)
        .build()
    );

    final var resources =
      this.resources();

    this.device =
      resources.add(
        physicalDevice.createLogicalDevice(deviceInfoBuilder.build())
      );

    this.queue =
      this.device.queues()
        .stream()
        .findFirst()
        .orElseThrow();

    final var vmaAllocators =
      VMALWJGLAllocatorProvider.create();

    this.vmaAllocator =
      resources.add(
        vmaAllocators.createAllocator(
          VMAAllocatorCreateInfo.builder()
            .setFrameInUseCount(1)
            .setLogicalDevice(this.device)
            .build()
        )
      );

    this.reconfigureForSize(context.width(), context.height());
    this.eventLifecycle(LOADING, 1.0, "");
    this.eventLifecycle(STARTED, 1.0, "");
    this.eventLifecycle(RUNNING, 1.0, "");
  }

  private void reconfigureForSize(
    final int width,
    final int height)
    throws VulkanException
  {
    this.frameResources = CloseableCollection.create();
    this.resources().add(this.frameResources);

    final var imageSizeBytes =
      ((long) width * 4L) * (long) height;

    final var outputBufferAllocation =
      this.vmaAllocator.createBuffer(
        VMAAllocationCreateInfo.builder()
          .setUsage(VMA_MEMORY_USAGE_GPU_TO_CPU)
          .addRequiredFlags(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT)
          .setMemoryTypeBits(0L)
          .build(),
        VulkanBufferCreateInfo.builder()
          .addUsageFlags(VK_BUFFER_USAGE_TRANSFER_DST_BIT)
          .setSize(imageSizeBytes)
          .setSharingMode(VK_SHARING_MODE_EXCLUSIVE)
          .build()
      );

    this.frameResources.add(outputBufferAllocation.result());

    final var outputBufferAllocationInfo =
      outputBufferAllocation.allocation().info();

    this.mappedFramebuffer =
      this.frameResources.add(
        this.device.mapMemory(
          outputBufferAllocationInfo.deviceMemory().orElseThrow(),
          outputBufferAllocationInfo.offset(),
          outputBufferAllocationInfo.size(),
          Set.of()
        ));

    final var framebufferImageAllocation =
      this.vmaAllocator.createImage(
        VMAAllocationCreateInfo.builder()
          .addRequiredFlags(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)
          .addFlags(VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT)
          .setUsage(VMA_MEMORY_USAGE_GPU_ONLY)
          .setMemoryTypeBits(0L)
          .build(),
        VulkanImageCreateInfo.builder()
          .addSamples(VK_SAMPLE_COUNT_1_BIT)
          .addUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
          .addUsage(VK_IMAGE_USAGE_TRANSFER_SRC_BIT)
          .setArrayLayers(1)
          .setExtent(VulkanExtent3D.of(width, height, 1))
          .setFormat(VK_FORMAT_A8B8G8R8_UNORM_PACK32)
          .setImageType(VK_IMAGE_TYPE_2D)
          .setInitialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
          .setMipLevels(1)
          .setSharingMode(VK_SHARING_MODE_EXCLUSIVE)
          .setTiling(VK_IMAGE_TILING_OPTIMAL)
          .build()
      );

    this.frameResources.add(framebufferImageAllocation.result());

    final var imageSubresourceRange =
      VulkanImageSubresourceRange.builder()
        .addAspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
        .setBaseArrayLayer(0)
        .setBaseMipLevel(0)
        .setLayerCount(1)
        .setLevelCount(1)
        .build();

    final var framebufferImageViewCreateInfo =
      VulkanImageViewCreateInfo.builder()
        .setComponents(VulkanComponentMappingType.identity())
        .setFormat(VK_FORMAT_A8B8G8R8_UNORM_PACK32)
        .setImage(framebufferImageAllocation.result())
        .setSubresourceRange(imageSubresourceRange)
        .setViewType(VulkanImageViewKind.VK_IMAGE_VIEW_TYPE_2D)
        .build();

    final var framebufferImageView =
      this.frameResources.add(
        this.device.createImageView(framebufferImageViewCreateInfo)
      );

    final var colorAttachmentDescription =
      VulkanAttachmentDescription.builder()
        .setFinalLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
        .setFormat(VK_FORMAT_A8B8G8R8_UNORM_PACK32)
        .setInitialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
        .setLoadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
        .setSamples(VK_SAMPLE_COUNT_1_BIT)
        .setStencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
        .setStencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
        .setStoreOp(VK_ATTACHMENT_STORE_OP_STORE)
        .build();

    final var colorReference =
      VulkanAttachmentReference.of(0, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

    final var subPass =
      VulkanSubpassDescription.builder()
        .setPipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
        .addColorAttachments(colorReference)
        .build();

    final var renderPassCreateInfo =
      VulkanRenderPassCreateInfo.builder()
        .addSubpasses(subPass)
        .addAttachments(colorAttachmentDescription)
        .build();

    final var renderPass =
      this.frameResources.add(
        this.device.createRenderPass(renderPassCreateInfo));

    final var commandPool =
      this.frameResources.add(
        this.device.createCommandPool(
          VulkanCommandPoolCreateInfo.builder()
            .setQueueFamilyIndex(this.queue.queueFamilyIndex())
            .build())
      );

    this.renderFence =
      this.frameResources.add(
        this.device.createFence(VulkanFenceCreateInfo.builder().build())
      );

    this.commandBuffer =
      this.frameResources.add(
        this.device.createCommandBuffer(
          commandPool, VK_COMMAND_BUFFER_LEVEL_PRIMARY)
      );

    final var renderArea =
      VulkanRectangle2D.builder()
        .setExtent(VulkanExtent2D.of(width, height))
        .setOffset(VulkanOffset2D.of(0, 0))
        .build();

    final var framebufferCreateInfo =
      VulkanFramebufferCreateInfo.builder()
        .setRenderPass(renderPass)
        .setWidth(width)
        .setHeight(height)
        .setLayers(1)
        .addAttachments(framebufferImageView)
        .build();

    final var framebuffer =
      this.frameResources.add(
        this.device.createFramebuffer(framebufferCreateInfo));

    final var red =
      VulkanClearValueColorFloatingPoint.of(1.0f, 0.0f, 0.0f, 1.0f);
    final var green =
      VulkanClearValueColorFloatingPoint.of(0.0f, 1.0f, 0.0f, 1.0f);

    final var renderPassBeginInfo =
      VulkanRenderPassBeginInfo.builder()
        .addClearValues(red)
        .setFramebuffer(framebuffer)
        .setRenderArea(renderArea)
        .setRenderPass(renderPass)
        .build();

    final var copyLayers =
      VulkanImageSubresourceLayers.builder()
        .addAspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
        .setLayerCount(1)
        .setBaseArrayLayer(0)
        .setMipLevel(0)
        .build();

    final var bufferImageCopy =
      VulkanBufferImageCopy.builder()
        .setBufferImageHeight(0)
        .setBufferOffset(0L)
        .setBufferRowLength(0)
        .setImageExtent(VulkanExtent3D.of(width, height, 1))
        .setImageOffset(VulkanOffset3D.of(0, 0, 0))
        .setImageSubresource(copyLayers)
        .build();

    this.commandBuffer.beginCommandBuffer();
    this.commandBuffer.beginRenderPass(
      renderPassBeginInfo,
      VK_SUBPASS_CONTENTS_INLINE);

    this.commandBuffer.clearAttachments(
      VulkanClearAttachment.builder()
        .setColorAttachment(0)
        .addAspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
        .setClearValue(green)
        .build(),
      VulkanClearRectangle.of(
        VulkanRectangle2D.of(
          VulkanOffset2D.of(0, 0),
          VulkanExtent2D.of(width, height)
        ),
        0,
        1
      ));

    this.commandBuffer.endRenderPass();
    this.commandBuffer.copyImageToBuffer(
      framebufferImageView.image(),
      VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
      outputBufferAllocation.result(),
      List.of(bufferImageCopy)
    );
    this.commandBuffer.endCommandBuffer();
  }

  @Override
  protected void onSizeChangedActual(
    final ExperimentContextType context)
    throws Exception
  {
    this.device.waitIdle();
    this.frameResources.close();
    this.reconfigureForSize(context.width(), context.height());
  }

  @Override
  protected void renderActual(
    final ExperimentContextType context,
    final ByteBuffer output)
    throws Exception
  {
    this.queue.submit(List.of(
      VulkanSubmitInfo.builder()
        .addCommandBuffers(this.commandBuffer)
        .build()
    ), Optional.of(this.renderFence));

    this.device.waitForFence(this.renderFence, 1_000_000_000L);
    this.device.resetFences(List.of(this.renderFence));

    final var source = this.mappedFramebuffer.asByteBuffer();
    source.position(0);
    output.position(0);
    output.put(source);
    output.position(0);
  }
}

