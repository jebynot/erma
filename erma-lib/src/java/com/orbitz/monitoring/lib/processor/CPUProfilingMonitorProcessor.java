package com.orbitz.monitoring.lib.processor;

import com.orbitz.monitoring.api.Monitor;
import com.orbitz.monitoring.api.MonitorProcessor;
import com.orbitz.monitoring.api.monitor.TransactionMonitor;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import org.apache.log4j.Logger;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * This is an implementation of the {@link MonitorProcessor} interface that uses a ThreadMXBean to
 * determine CPU time used by the current thread within the scope of a TransactionMonitor.
 * 
 * @author Matt O'Keefe
 * 
 * @@org.springframework.jmx.export.metadata.ManagedResource 
 *                                                           (description="This MonitorProcessor can be dis/enabled."
 *                                                           )
 */
@ManagedResource(description = "This MonitorProcessor can be dis/enabled.")
public class CPUProfilingMonitorProcessor extends MonitorProcessorAdapter {
  
  private static final Logger log = Logger.getLogger(CPUProfilingMonitorProcessor.class);
  
  private boolean enabled = false;
  
  // ** PUBLIC METHODS ******************************************************
  @Override
  public void monitorStarted(final Monitor monitor) {
    
    if (enabled && TransactionMonitor.class.isAssignableFrom(monitor.getClass())) {
      ThreadMXBean tmxbean = ManagementFactory.getThreadMXBean();
      monitor.set("startCPUTime", tmxbean.getCurrentThreadCpuTime());
    }
  }
  
  @Override
  public void process(final Monitor monitor) {
    
    if (enabled && TransactionMonitor.class.isAssignableFrom(monitor.getClass())) {
      ThreadMXBean tmxbean = ManagementFactory.getThreadMXBean();
      long endTime = tmxbean.getCurrentThreadCpuTime();
      monitor.set("endCPUTime", endTime);
      long diff = endTime - monitor.getAsLong("startCPUTime");
      double cpuTimeMillis = diff / 1000000.0;
      if (log.isDebugEnabled()) {
        log.debug("cpuTimeMillis=" + cpuTimeMillis);
      }
      monitor.set("cpuTimeMillis", cpuTimeMillis);
    }
  }
  
  /**
   * @@org.springframework.jmx.export.metadata.ManagedAttribute 
   *                                                            (description="true if this feature is enabled"
   *                                                            )
   * 
   * @return boolean
   */
  @ManagedAttribute(description = "true if this feature is enabled")
  public boolean isEnabled() {
    return enabled;
  }
  
  /**
   * Enables or disables the {@link CPUProfilingMonitorProcessor}
   * @param enabled true if the processor should be enabled, false otherwise
   * @@org.springframework.jmx.export.metadata.ManagedAttribute 
   *                                                            (description="set to true to enable this feature"
   *                                                            )
   */
  @ManagedAttribute(description = "set to true to enable this feature")
  public void setEnabled(final boolean enabled) {
    ThreadMXBean tmxbean = ManagementFactory.getThreadMXBean();
    if (tmxbean.isCurrentThreadCpuTimeSupported()) {
      tmxbean.setThreadCpuTimeEnabled(enabled);
      this.enabled = enabled;
    }
    else {
      log.warn("Thread CPU time monitoring is not supported by this VM");
    }
  }
}
