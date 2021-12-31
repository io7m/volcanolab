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

import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import com.io7m.volcanolab.experiment.api.ExperimentContextType;
import com.io7m.volcanolab.experiment.api.ExperimentError;
import com.io7m.volcanolab.experiment.api.ExperimentEventLifecycle;
import com.io7m.volcanolab.experiment.api.ExperimentEventType;
import com.io7m.volcanolab.experiment.api.ExperimentLifecycleStatus;
import com.io7m.volcanolab.experiment.api.ExperimentType;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Objects;

import static com.io7m.volcanolab.experiment.api.ExperimentLifecycleStatus.STOPPED;

public abstract class ExperimentAbstract implements ExperimentType
{
  private final Logger logger;
  private final String name;
  private final Subject<ExperimentEventType> events;
  private boolean failed;
  private CloseableCollectionType<ClosingResourceFailedException> resources;

  protected ExperimentAbstract(
    final Logger inLogger,
    final String inName)
  {
    this.logger =
      Objects.requireNonNull(inLogger, "logger");
    this.name =
      Objects.requireNonNull(inName, "name");
    this.events =
      PublishSubject.create();
    this.resources =
      CloseableCollection.create();

    this.failed = false;
  }

  protected final void event(
    final ExperimentEventType event)
  {
    this.events.onNext(Objects.requireNonNull(event, "event"));
  }

  @Override
  public final void close()
    throws Exception
  {
    if (this.logger.isDebugEnabled()) {
      this.logger.debug("close");
    }

    this.eventLifecycle(STOPPED, 1.0, "");
    this.closeActual();
    this.resources.close();
  }

  protected abstract void closeActual();

  protected final void eventLifecycle(
    final ExperimentLifecycleStatus status,
    final double progress,
    final String message)
  {
    this.event(new ExperimentEventLifecycle(status, progress, message));
  }

  @Override
  public final String name()
  {
    return this.name;
  }

  @Override
  public final Observable<ExperimentEventType> events()
  {
    return this.events;
  }

  @Override
  public final void start(
    final ExperimentContextType context)
    throws Exception
  {
    Objects.requireNonNull(context, "context");

    if (this.logger.isDebugEnabled()) {
      this.logger.debug("start");
    }

    try {
      this.failed = false;
      this.startActual(context);
    } catch (final Exception e) {
      this.failed = true;
      this.event(new ExperimentError(e));
      throw e;
    }
  }

  protected abstract void startActual(
    ExperimentContextType context)
    throws Exception;

  @Override
  public final void onSizeChanged(
    final ExperimentContextType context)
    throws Exception
  {
    Objects.requireNonNull(context, "context");

    if (this.logger.isDebugEnabled()) {
      this.logger.debug(
        "onSizeChanged: {}x{}",
        context.width(),
        context.height());
    }

    if (this.failed) {
      return;
    }

    try {
      this.onSizeChangedActual(context);
    } catch (final Exception e) {
      this.failed = true;
      this.event(new ExperimentError(e));
      throw e;
    }
  }

  protected abstract void onSizeChangedActual(
    ExperimentContextType context)
    throws Exception;

  @Override
  public final void render(
    final ExperimentContextType context,
    final ByteBuffer output)
    throws Exception
  {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(output, "output");

    if (this.failed) {
      return;
    }

    try {
      this.renderActual(context, output);
    } catch (final Exception e) {
      this.failed = true;
      this.event(new ExperimentError(e));
      throw e;
    }
  }

  protected abstract void renderActual(
    ExperimentContextType context,
    ByteBuffer output)
    throws Exception;

  protected final CloseableCollectionType<?> resources()
  {
    return this.resources;
  }
}
