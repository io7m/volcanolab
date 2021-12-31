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

import com.io7m.volcanolab.experiment.api.ExperimentContextType;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static com.io7m.volcanolab.experiment.api.ExperimentLifecycleStatus.INITIALIZED;
import static com.io7m.volcanolab.experiment.api.ExperimentLifecycleStatus.LOADING;
import static com.io7m.volcanolab.experiment.api.ExperimentLifecycleStatus.RUNNING;
import static com.io7m.volcanolab.experiment.api.ExperimentLifecycleStatus.STARTED;

public final class ExperimentSlowLoad extends ExperimentAbstract
{
  private double progress;

  public ExperimentSlowLoad()
  {
    super(LoggerFactory.getLogger(ExperimentSlowLoad.class), "SlowLoad");
    this.progress = 0.0;
  }

  @Override
  protected void closeActual()
  {

  }

  @Override
  protected void startActual(
    final ExperimentContextType context)
  {
    this.progress = 0.0;
    this.eventLifecycle(INITIALIZED, 0.0, "");
    this.eventLifecycle(STARTED, 1.0, "");
  }

  @Override
  protected void onSizeChangedActual(
    final ExperimentContextType context)
  {

  }

  @Override
  protected void renderActual(
    final ExperimentContextType context,
    final ByteBuffer output)
  {
    if (this.progress >= 1.0) {
      this.eventLifecycle(RUNNING, 1.0, "");
    } else {
      this.progress += 0.002;
      this.eventLifecycle(LOADING, this.progress, "");
    }

    final var blue = (byte) (this.progress * 255.0);
    for (int index = 0; index < output.capacity(); index += 4) {
      output.put(index, blue);
      output.put(index + 1, (byte) 0x00);
      output.put(index + 2, (byte) 0x00);
      output.put(index + 3, (byte) 0xff);
    }
  }
}

