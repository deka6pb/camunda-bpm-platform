/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.application;

import java.util.concurrent.Callable;

import org.activiti.engine.repository.DeploymentBuilder;
import org.camunda.bpm.container.impl.RuntimeContainerConfiguration;
import org.camunda.bpm.container.spi.RuntimeContainerDelegate;
import org.camunda.bpm.engine.impl.util.ClassLoaderUtil;


/**
 * <p>A ProcessApplication is a Java Application that creates a deployment 
 * to an Embedded or Shared ProcessEngine.</p> 
 * 
 * @author Daniel Meyer
 *
 */
public abstract class AbstractProcessApplication {
              
  // lifecycle /////////////////////////////////////////////////////

  /**
   * Start this {@link AbstractProcessApplication}. 
   */
  public void start() {
    // delegate starting of the process application to the runtime container.
    getRuntimeContainerDelegate().deployProcessApplication(this);   
  }
  
  /**
   * Stop this {@link AbstractProcessApplication}
   */
  public void stop() {
    // delegate stopping of the process application to the runtime container.
    getRuntimeContainerDelegate().undeployProcessApplication(this);
  }

  private RuntimeContainerDelegate getRuntimeContainerDelegate() {
    return RuntimeContainerConfiguration.getInstance().getContainerDelegate();
  }
  
  // Deployment //////////////////////////////////////////////////
  
  /**
   * <p>Override this method in order to programmatically add resources to the
   * deployment created by this process application.</p>
   * 
   * <p>This method is invoked at deployment time once for each process archive 
   * deployed by this process application.</p>
   * 
   * <p><strong>NOTE:</strong> this method must NOT call the {@link DeploymentBuilder#deploy()} 
   * method.</p>
   * 
   * @param deploymentBuilder the {@link DeploymentBuilder} used to construct the deployment.
   * @param processArchiveName the name of the processArchive which is currently being deployed.
   */
  public void createDeployment(String processArchiveName, DeploymentBuilder deploymentBuilder) {
    // default implementation does nothing
  }
    
  // Runtime ////////////////////////////////////////////
  
  /**
   * @return the name of this process application
   */
  public String getName() {
    Class<? extends AbstractProcessApplication> processApplicationClass = getClass();
    String name = null;
    
    try {
      ProcessApplication annotation = processApplicationClass.getAnnotation(ProcessApplication.class);      
      name = annotation.value();
    } catch(NullPointerException nullPointerException) {
      // ignore
    }
    
    if(name == null || name.length()==0) {
      name = autodetectProcessApplicationName();
    }    
    
    return name;    
  }
  
  /**
   * override this method to autodetect an application name in case the
   * {@link ProcessApplication} annotation was used but without parameter.
   */
  protected abstract String autodetectProcessApplicationName();
   
  /**
   * <p>Returns a globally sharable reference to this process application. This reference may be safely passed 
   * to the process engine. And other applications.</p>  
   * 
   * @return a globally sharable reference to this process application. 
   */
  public abstract ProcessApplicationReference getReference();

  /**
   * The default implementation simply modifies the Context {@link ClassLoader}
   * 
   * @param callable the callable to be executed "within" the context of this process application.
   * @return the result of the callback
   * @throws Exception 
   */
  public <T> T execute(Callable<T> callable) throws ProcessApplicationExecutionException {
    ClassLoader originalClassloader = ClassLoaderUtil.getContextClassloader();
    
    ClassLoader processApplicationClassloader = getProcessApplicationClassloader();
    
    try {
      ClassLoaderUtil.setContextClassloader(processApplicationClassloader);
      
      return callable.call();
      
    } catch(Exception e) {
      throw new ProcessApplicationExecutionException(e);
      
    } finally {
      ClassLoaderUtil.setContextClassloader(originalClassloader);
    }
    
  }

  /**
   * <p>Override this method to provide an environment-specific {@link ClassLoader} to be used by the process 
   * engine for loading resources from the process applicaiton</p>
   * 
   * <p><strong>NOTE: the process engine must <em>never</em> cache any references to this {@link ClassLoader} 
   * or to classes obtained through this {@link ClassLoader}.</strong></p>
   * 
   * @return the {@link ClassLoader} that can be used to load classes and resources from this process application.
   */
  public ClassLoader getProcessApplicationClassloader() {
    // the default implementation uses the classloader that loaded 
    // the application-provided subclass of this class.    
    return getClass().getClassLoader();
  }
  
}
