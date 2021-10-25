package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final ProfilingState state;
  private final Object delegate;

  // TODO: You will need to add more instance fields and constructor arguments to this class.
  ProfilingMethodInterceptor(Clock clock, ProfilingState state, Object delegate) {
    this.clock = Objects.requireNonNull(clock);
    this.state = Objects.requireNonNull(state);
    this.delegate = Objects.requireNonNull(delegate);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // TODO: This method interceptor should inspect the called method to see if it is a profiled
    //       method. For profiled methods, the interceptor should record the start time, then
    //       invoke the method using the object that is being profiled. Finally, for profiled
    //       methods, the interceptor should record how long the method call took, using the
    //       ProfilingState methods.
    Object invokeReturn = null;

    if(method.getAnnotation(Profiled.class)!=null){ //Annotated Methods
      Instant startTime = clock.instant();
      try{
        invokeReturn = method.invoke(delegate,args);
      }catch (Throwable throwable){
        throw throwable.getCause();
      } finally {
        Instant endTime = clock.instant();
        Duration duration = Duration.between(startTime,endTime);
        state.record(delegate.getClass(),method,duration);
      }
    }else { //Default methods (no need to record in state)
      try{
        invokeReturn = method.invoke(delegate,args);
      }catch (Throwable throwable){
        throw throwable.getCause();
      }
    }

    return invokeReturn;
  }
}
