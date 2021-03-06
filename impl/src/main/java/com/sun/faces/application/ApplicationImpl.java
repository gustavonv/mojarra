/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package com.sun.faces.application;

import static java.util.logging.Level.FINE;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.el.CompositeELResolver;
import javax.el.ELContextListener;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.NavigationHandler;
import javax.faces.application.ProjectStage;
import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.application.StateManager;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIComponent;
import javax.faces.component.behavior.Behavior;
import javax.faces.component.search.SearchExpressionHandler;
import javax.faces.component.search.SearchKeywordResolver;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.el.MethodBinding;
import javax.faces.el.PropertyResolver;
import javax.faces.el.ReferenceSyntaxException;
import javax.faces.el.ValueBinding;
import javax.faces.el.VariableResolver;
import javax.faces.event.ActionListener;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import javax.faces.flow.FlowHandler;
import javax.faces.validator.Validator;

import com.sun.faces.application.applicationimpl.Events;
import com.sun.faces.application.applicationimpl.ExpressionLanguage;
import com.sun.faces.application.applicationimpl.InstanceFactory;
import com.sun.faces.application.applicationimpl.SearchExpression;
import com.sun.faces.application.applicationimpl.Singletons;
import com.sun.faces.application.applicationimpl.Stage;
import com.sun.faces.el.FacesCompositeELResolver;
import com.sun.faces.util.FacesLogger;

/**
 * <p>
 * <strong>Application</strong> represents a per-web-application singleton object where applications
 * based on JavaServer Faces (or implementations wishing to provide extended functionality) can
 * register application-wide singletons that provide functionality required by JavaServer Faces.
 */
public class ApplicationImpl extends Application {

    public static final String THIS_LIBRARY = "com.sun.faces.composite.this.library";

    // Log instance for this class
    private static final Logger LOGGER = FacesLogger.APPLICATION.getLogger();

    // Relationship Instance Variables

    private final ApplicationAssociate associate;
    private final Events events;
    private final Singletons singletons;
    private final ExpressionLanguage expressionLanguage;
    private final InstanceFactory instanceFactory;
    private final SearchExpression searchExpression;
    private final Stage stage;
   

    /**
     * Constructor
     */
    public ApplicationImpl() {
        // stage and events are called back by ApplicationAssociate
        stage = new Stage();
        events = new Events();
        associate = new ApplicationAssociate(this);
        singletons = new Singletons(associate);
        expressionLanguage = new ExpressionLanguage(associate);
        instanceFactory = new InstanceFactory(associate);
        searchExpression = new SearchExpression(associate);
        
        if (LOGGER.isLoggable(FINE)) {
            LOGGER.log(FINE, "Created Application instance ");
        }
    }
    
    
    // ----------------------------------------------------------- Events
    

    /**
     * @see javax.faces.application.Application#publishEvent(FacesContext, Class, Object)
     */
    @Override
    public void publishEvent(FacesContext context, Class<? extends SystemEvent> systemEventClass, Object source) {
        publishEvent(context, systemEventClass, null, source);
    }

    /**
     * @see javax.faces.application.Application#publishEvent(FacesContext, Class, Object)
     */
    @Override
    public void publishEvent(FacesContext context, Class<? extends SystemEvent> systemEventClass, Class<?> sourceBaseType, Object source) {
        events.publishEvent(context, systemEventClass, sourceBaseType, source, getProjectStage());
    }
    
    /**
     * @see Application#subscribeToEvent(Class, javax.faces.event.SystemEventListener)
     */
    @Override
    public void subscribeToEvent(Class<? extends SystemEvent> systemEventClass, SystemEventListener listener) {
        subscribeToEvent(systemEventClass, null, listener);
    }

    /**
     * @see Application#subscribeToEvent(Class, Class, javax.faces.event.SystemEventListener)
     */
    @Override
    public void subscribeToEvent(Class<? extends SystemEvent> systemEventClass, Class<?> sourceClass, SystemEventListener listener) {
        events.subscribeToEvent(systemEventClass, sourceClass, listener);
    }
    
    /**
     * @see Application#unsubscribeFromEvent(Class, javax.faces.event.SystemEventListener)
     */
    @Override
    public void unsubscribeFromEvent(Class<? extends SystemEvent> systemEventClass, SystemEventListener listener) {
        unsubscribeFromEvent(systemEventClass, null, listener);
    }

    /**
     * @see Application#unsubscribeFromEvent(Class, Class, javax.faces.event.SystemEventListener)
     */
    @Override
    public void unsubscribeFromEvent(Class<? extends SystemEvent> systemEventClass, Class<?> sourceClass, SystemEventListener listener) {
       events.unsubscribeFromEvent(systemEventClass, sourceClass, listener);
    }
    
    
    
    // ----------------------------------------------------------- Expression language
    

    /**
     * @see javax.faces.application.Application#addELContextListener(javax.el.ELContextListener)
     */
    @Override
    public void addELContextListener(ELContextListener listener) {
        expressionLanguage.addELContextListener(listener);
    }

    /**
     * @see javax.faces.application.Application#removeELContextListener(javax.el.ELContextListener)
     */
    @Override
    public void removeELContextListener(ELContextListener listener) {
        expressionLanguage.removeELContextListener(listener);
    }

    /**
     * @see javax.faces.application.Application#getELContextListeners()
     */
    @Override
    public ELContextListener[] getELContextListeners() {
        return expressionLanguage.getELContextListeners();
    }

    /**
     * @see javax.faces.application.Application#getExpressionFactory()
     */
    @Override
    public ExpressionFactory getExpressionFactory() {
        return expressionLanguage.getExpressionFactory();
    }
   
    /**
     * @see javax.faces.application.Application#evaluateExpressionGet(javax.faces.context.FacesContext,
     *      String, Class)
     */
    @Override
    public <T> T evaluateExpressionGet(FacesContext context, String expression, Class<? extends T> expectedType) throws ELException {
       return expressionLanguage.evaluateExpressionGet(context, expression, expectedType);
    }

    /**
     * @see javax.faces.application.Application#getELResolver()
     */
    @Override
    public ELResolver getELResolver() {
        return expressionLanguage.getELResolver();
    }

    /**
     * @see javax.faces.application.Application#addELResolver(javax.el.ELResolver)
     */
    @Override
    public void addELResolver(ELResolver resolver) {
        expressionLanguage.addELResolver(resolver);
    }
    
    public CompositeELResolver getApplicationELResolvers() {
        return expressionLanguage.getApplicationELResolvers();
    }
    
    public FacesCompositeELResolver getCompositeELResolver() {
        return expressionLanguage.getCompositeELResolver();
    }

    public void setCompositeELResolver(FacesCompositeELResolver compositeELResolver) {
        expressionLanguage.setCompositeELResolver(compositeELResolver);
    }
    
    
    
    // ----------------------------------------------------------- Singletons
    

    /**
     * @see javax.faces.application.Application#getViewHandler()
     */
    @Override
    public ViewHandler getViewHandler() {
        return singletons.getViewHandler();
    }

    /**
     * @see javax.faces.application.Application#setViewHandler(javax.faces.application.ViewHandler)
     */
    @Override
    public void setViewHandler(ViewHandler viewHandler) {
        singletons.setViewHandler(viewHandler);
    }

    /**
     * @see javax.faces.application.Application#getResourceHandler()
     */
    @Override
    public ResourceHandler getResourceHandler() {
        return singletons.getResourceHandler();
    }

    /**
     * @see javax.faces.application.Application#setResourceHandler(javax.faces.application.ResourceHandler)
     */
    @Override
    public void setResourceHandler(ResourceHandler resourceHandler) {
        singletons.setResourceHandler(resourceHandler);
    }

    /**
     * @see javax.faces.application.Application#getStateManager()
     */
    @Override
    public StateManager getStateManager() {
        return singletons.getStateManager();
    }

    /**
     * @see javax.faces.application.Application#setStateManager(javax.faces.application.StateManager)
     */
    @Override
    public void setStateManager(StateManager stateManager) {
        singletons.setStateManager(stateManager);
    }
    
    /**
     * @see javax.faces.application.Application#getActionListener()
     */
    @Override
    public ActionListener getActionListener() {
        return singletons.getActionListener();
    }

    /**
     * @see Application#setActionListener(javax.faces.event.ActionListener)
     */
    @Override
    public void setActionListener(ActionListener actionListener) {
        singletons.setActionListener(actionListener);
    }

    /**
     * @see javax.faces.application.Application#getNavigationHandler()
     */
    @Override
    public NavigationHandler getNavigationHandler() {
        return singletons.getNavigationHandler();
    }

    /**
     * @see javax.faces.application.Application#setNavigationHandler(javax.faces.application.NavigationHandler)
     */
    @Override
    public void setNavigationHandler(NavigationHandler navigationHandler) {
        singletons.setNavigationHandler(navigationHandler);
    }
    
    @Override
    public FlowHandler getFlowHandler() {
        return singletons.getFlowHandler();
    }

    @Override
    public void setFlowHandler(FlowHandler flowHandler) {
        singletons.setFlowHandler(flowHandler);
    }
    
    /**
     * @see javax.faces.application.Application#getSupportedLocales()
     */
    @Override
    public Iterator<Locale> getSupportedLocales() {
      return singletons.getSupportedLocales();
    }

    /**
     * @see javax.faces.application.Application#setSupportedLocales(java.util.Collection)
     */
    @Override
    public void setSupportedLocales(Collection<Locale> newLocales) {
        singletons.setSupportedLocales(newLocales);
    }

    /**
     * @see javax.faces.application.Application#getDefaultLocale()
     */
    @Override
    public Locale getDefaultLocale() {
        return singletons.getDefaultLocale();
    }

    /**
     * @see javax.faces.application.Application#setDefaultLocale(java.util.Locale)
     */
    @Override
    public void setDefaultLocale(Locale locale) {
        singletons.setDefaultLocale(locale);
    }
    
    /**
     * @see javax.faces.application.Application#setMessageBundle(String)
     */
    @Override
    public void setMessageBundle(String messageBundle) {
      singletons.setMessageBundle(messageBundle);
    }

    /**
     * @see javax.faces.application.Application#getMessageBundle()
     */
    @Override
    public String getMessageBundle() {
        return singletons.getMessageBundle();
    }
    
    
    /**
     * @see javax.faces.application.Application#getDefaultRenderKitId()
     */
    @Override
    public String getDefaultRenderKitId() {
        return singletons.getDefaultRenderKitId();
    }

    /**
     * @see javax.faces.application.Application#setDefaultRenderKitId(String)
     */
    @Override
    public void setDefaultRenderKitId(String renderKitId) {
        singletons.setDefaultRenderKitId(renderKitId);
    }
    
    /**
     * @see javax.faces.application.Application#getResourceBundle(javax.faces.context.FacesContext,
     *      String)
     */
    @Override
    public ResourceBundle getResourceBundle(FacesContext context, String var) {
        return singletons.getResourceBundle(context, var);
    }

    
    
    // ----------------------------------------------------------- Instance factory
    
    
    /**
     * @see javax.faces.application.Application#addBehavior(String, String)
     */
    @Override
    public void addBehavior(String behaviorId, String behaviorClass) {
       instanceFactory.addBehavior(behaviorId, behaviorClass);
    }

    /**
     * @see javax.faces.application.Application#createBehavior(String)
     */
    @Override
    public Behavior createBehavior(String behaviorId) throws FacesException {
        return instanceFactory.createBehavior(behaviorId);
    }

    /**
     * @see javax.faces.application.Application#getBehaviorIds()
     */
    @Override
    public Iterator<String> getBehaviorIds() {
        return instanceFactory.getBehaviorIds();
    }
    
    @Override
    public UIComponent createComponent(String componentType) throws FacesException {
        return instanceFactory.createComponent(componentType);
    }

    /**
     * @see javax.faces.application.Application#addComponent(java.lang.String, java.lang.String)
     */
    @Override
    public void addComponent(String componentType, String componentClass) {
        instanceFactory.addComponent(componentType, componentClass);
    }
    
    @Override
    public UIComponent createComponent(ValueExpression componentExpression, FacesContext context, String componentType) throws FacesException {
        return instanceFactory.createComponent(componentExpression, context, componentType);
    }
    
    @Override
    public UIComponent createComponent(ValueExpression componentExpression, FacesContext context, String componentType, String rendererType) {
        return instanceFactory.createComponent(componentExpression, context, componentType, rendererType);
    }
    
    @Override
    public UIComponent createComponent(FacesContext context, String componentType, String rendererType) {
        return instanceFactory.createComponent(context, componentType, rendererType);
    }

    @Override
    public UIComponent createComponent(FacesContext context, Resource componentResource) throws FacesException {
        return instanceFactory.createComponent(context, componentResource, getExpressionFactory());
    }

    @SuppressWarnings("deprecation")
    @Override
    public UIComponent createComponent(ValueBinding componentBinding, FacesContext context, String componentType) throws FacesException {
        return instanceFactory.createComponent(componentBinding, context, componentType);
    }

    /**
     * @see javax.faces.application.Application#getComponentTypes()
     */
    @Override
    public Iterator<String> getComponentTypes() {
        return instanceFactory.getComponentTypes();
    }

    /**
     * @see javax.faces.application.Application#addConverter(String, String)
     */
    @Override
    public void addConverter(String converterId, String converterClass) {
        instanceFactory.addConverter(converterId, converterClass);
    }

    /**
     * @see javax.faces.application.Application#addConverter(Class, String)
     */
    @Override
    public void addConverter(Class<?> targetClass, String converterClass) {
       instanceFactory.addConverter(targetClass, converterClass);
    }

    /**
     * @see javax.faces.application.Application#createConverter(String)
     */
    @Override
    public Converter<?> createConverter(String converterId) {
        return instanceFactory.createConverter(converterId);
    }

    /**
     * @see javax.faces.application.Application#createConverter(Class)
     */
    @Override
    public Converter<?> createConverter(Class<?> targetClass) {
        return instanceFactory.createConverter(targetClass);
    }

    /**
     * @see javax.faces.application.Application#getConverterIds()
     */
    @Override
    public Iterator<String> getConverterIds() {
        return instanceFactory.getConverterIds();
    }

    /**
     * @see javax.faces.application.Application#getConverterTypes()
     */
    @Override
    public Iterator<Class<?>> getConverterTypes() {
        return instanceFactory.getConverterTypes();
    }

    /**
     * @see javax.faces.application.Application#addValidator(String, String)
     */
    @Override
    public void addValidator(String validatorId, String validatorClass) {
      instanceFactory.addValidator(validatorId, validatorClass);
    }

    /**
     * @see javax.faces.application.Application#createValidator(String)
     */
    @Override
    public Validator<?> createValidator(String validatorId) throws FacesException {
        return instanceFactory.createValidator(validatorId);
    }

    /**
     * @see javax.faces.application.Application#getValidatorIds()
     */
    @Override
    public Iterator<String> getValidatorIds() {
        return instanceFactory.getValidatorIds();
    }

    /**
     * @see javax.faces.application.Application#addDefaultValidatorId(String)
     */
    @Override
    public void addDefaultValidatorId(String validatorId) {
        instanceFactory.addDefaultValidatorId(validatorId);
    }

    /**
     * @see javax.faces.application.Application#getDefaultValidatorInfo()
     */
    @Override
    public Map<String, String> getDefaultValidatorInfo() {
        return instanceFactory.getDefaultValidatorInfo();
    }
    
    
    // ----------------------------------------------------------- Instance factory
    
    /**
     * @see javax.faces.application.Application#getProjectStage()
     */
    @Override
    public ProjectStage getProjectStage() {
        return stage.getProjectStage(this);
    }
    
    
    
    // ----------------------------------------------------------- Search expression
   
    
    @Override
    public SearchExpressionHandler getSearchExpressionHandler() {
        return searchExpression.getSearchExpressionHandler();
    }

    @Override
    public void setSearchExpressionHandler(SearchExpressionHandler searchExpressionHandler) {
       searchExpression.setSearchExpressionHandler(searchExpressionHandler);
    }

    @Override
    public void addSearchKeywordResolver(SearchKeywordResolver resolver) {
        searchExpression.addSearchKeywordResolver(resolver);
    }

    @Override
    public SearchKeywordResolver getSearchKeywordResolver() {
        return searchExpression.getSearchKeywordResolver();
    }

    
    


 
    
    
    // ----------------------------------------------------------- Deprecated methods
    
    
    /**
     * @see javax.faces.application.Application#setPropertyResolver(javax.faces.el.PropertyResolver)
     */
    @Override
    @Deprecated
    public PropertyResolver getPropertyResolver() {
        return expressionLanguage.getPropertyResolver();
    }
    
    /**
     * @see javax.faces.application.Application#setPropertyResolver(javax.faces.el.PropertyResolver)
     */
    @Override
    @Deprecated
    public void setPropertyResolver(PropertyResolver resolver) {
        expressionLanguage.setPropertyResolver(resolver);
    }

    /**
     * @see javax.faces.application.Application#createMethodBinding(String, Class[])
     */
    @Override
    @Deprecated
    public MethodBinding createMethodBinding(String ref, Class<?> params[]) {
        return expressionLanguage.createMethodBinding(ref, params);
    }

    /**
     * @see javax.faces.application.Application#createValueBinding(String)
     */
    @Override
    @Deprecated
    public ValueBinding createValueBinding(String ref) throws ReferenceSyntaxException {
        return expressionLanguage.createValueBinding(ref);
    }

    /**
     * @see javax.faces.application.Application#getVariableResolver()
     */
    @Override
    @Deprecated
    public VariableResolver getVariableResolver() {
        return expressionLanguage.getVariableResolver();
    }

    /**
     * @see javax.faces.application.Application#setVariableResolver(javax.faces.el.VariableResolver)
     */
    @Override
    @Deprecated
    public void setVariableResolver(VariableResolver resolver) {
        expressionLanguage.setVariableResolver(resolver);
    }

}
