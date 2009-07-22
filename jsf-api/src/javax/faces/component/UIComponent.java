/*
 * $Id: UIComponent.java,v 1.153.8.13 2008/04/17 18:51:28 edburns Exp $
 */

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
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

package javax.faces.component;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.FacesWrapper;
import javax.faces.application.Resource;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitHint;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.PostRestoreStateEvent;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ComponentSystemEventListener;
import javax.faces.event.FacesEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import javax.faces.event.FacesListener;
import javax.faces.event.SystemEventListenerHolder;
import javax.faces.render.Renderer;

/**
 * <p><strong class="changed_modified_2_0">UIComponent</strong> is the
 * base class for all user interface components in JavaServer Faces.
 * The set of {@link UIComponent} instances associated with a particular
 * request and response are organized into a component tree under a
 * {@link UIViewRoot} that represents the entire content of the request
 * or response.</p>
 *
 * <p>For the convenience of component developers,
 * {@link UIComponentBase} provides the default
 * behavior that is specified for a {@link UIComponent}, and is the base class
 * for all of the concrete {@link UIComponent} "base" implementations.
 * Component writers are encouraged to subclass
 * {@link UIComponentBase}, instead of directly
 * implementing this abstract class, to reduce the impact of any future changes
 * to the method signatures.</p>
 *
 * <p class="changed_added_2_0">If the {@link
 * javax.faces.event.ListenerFor} annotation is attached to the class
 * definition of a <code>Component</code>, that class must also
 * implement {@link javax.faces.event.ComponentSystemEventListener}.
 * </p>

 */

public abstract class UIComponent implements PartialStateHolder, SystemEventListenerHolder,
        ComponentSystemEventListener {

    private static Logger LOGGER = Logger.getLogger("javax.faces.component",
            "javax.faces.LogStrings");
    
    /**
     * <p class="changed_added_2_0">The key to which the
     * <code>UIComponent</code> currently being processed will be
     * associated with within the {@link FacesContext} attributes map.</p>
     *
     * @see javax.faces.context.FacesContext#getAttributes()
     *
     * @since 2.0
     */
    public static final String CURRENT_COMPONENT = "javax.faces.component.CURRENT_COMPONENT";

    /**
     * <p class="changed_added_2_0">The key to which the
     * <em>composite</em> <code>UIComponent</code> currently being
     * processed will be associated with within the {@link FacesContext}
     * attributes map.</p>
     *
     * @see javax.faces.context.FacesContext#getAttributes()
     *
     * @since 2.0
     */
    public static final String CURRENT_COMPOSITE_COMPONENT = "javax.faces.component.CURRENT_COMPOSITE_COMPONENT";

    /**
     * <p class="changed_added_2_0">The value of this constant is used as the key in the
     * component attribute map, the value for which is a
     * <code>java.beans.BeanInfo</code> implementation describing the composite
     * component.  This <code>BeanInfo</code> is known as the 
     * <em>composite component BeanInfo</em>.</p>
     *
     * @since 2.0
     */
    public static final String BEANINFO_KEY = "javax.faces.component.BEANINFO_KEY";


    /**
     * <p class="changed_added_2_0">The value of this constant is used as the key
     * in the <em>composite component BeanDescriptor</em> for the 
     * <code>Map&lt;PropertyDescriptor&gt;</code> that contains meta-information
     * for the declared facets for this composite component.
     * This map must contain an entry under the key {@link #COMPOSITE_FACET_NAME}, even
     * if no facets were explicitly declared.  See {@link #COMPOSITE_FACET_NAME}.</p>
     *
     * @since 2.0
     */
    public static final String FACETS_KEY = "javax.faces.component.FACETS_KEY";
    
    /**
     * <p class="changed_added_2_0">The value of this constant is used as the key
     * in the component attributes <code>Map</code> for the 
     * {@link javax.faces.view.Location} in the view at which this component 
     * instance resides.</p>
     *
     * @since 2.0
     */
    public static final String VIEW_LOCATION_KEY = "javax.faces.component.VIEW_LOCATION_KEY";
    
    /**
     * <p class="changed_added_2_0">The value of this constant is used as the key
     * in the <em>composite component BeanDescriptor</em> for a 
     * <code>ValueExpression</code> that evaluates to the 
     * <code>component-type</code> of the <em>composite component root</em>
     * <code>UIComponent</code> for this composite component, if
     * one was declared by the composite component author.</p>
     *
     * @since 2.0
     */
    public static final String COMPOSITE_COMPONENT_TYPE_KEY = "javax.faces.component.COMPOSITE_COMPONENT_TYPE";
    
    /**
     * <p class="changed_added_2_0">The value of this constant is used as the key
     * in the <code>Map</code> returned as described in {@link #FACETS_KEY}
     * for the 
     * <code>PropertyDescriptor</code> describing the composite component facet.
     * The value of this constant is also used as the key in the <code>Map</code>
     * returned from {@link #getFacets}.  In this case, it refers to the actual
     * facet that is the {@link javax.faces.component.UIPanel} that is the parent of the all
     * of the components in the <code>&lt;composite:implementation&gt;</code>
     * section of the <em>composite component VDL file</em>.</p>
     *
     * @since 2.0
     */
    public static final String COMPOSITE_FACET_NAME = "javax.faces.component.COMPOSITE_FACET_NAME";

    enum PropertyKeysPrivate {
        attributesThatAreSet
    }

    /**
     * Properties that are tracked by state saving.
     */
    enum PropertyKeys {
        rendered,
        attributes,
        bindings,
        rendererType,
        systemEventListeners,
        behaviors
    }

    /**
     * List of attributes that have been set on the component (this
     * may be from setValueExpression, the attributes map, or setters
     * from the concrete HTML components.  This allows
     * for faster rendering of attributes as this list is authoratative
     * on what has been set.
     */
    List<String> attributesThatAreSet;
    StateHelper stateHelper = null;
    UIComponent compositeParent;


    // -------------------------------------------------------------- Attributes


    /**
     * <p>Return a mutable 
     * <code>Map</code> representing the attributes
     * (and properties, see below) associated wth this {@link UIComponent},
     * keyed by attribute name (which must be a String).  The returned
     * implementation must support all of the standard and optional
     * <code>Map</code> methods, plus support the following additional
     * requirements:</p>
     * <ul>
     * <li>The <code>Map</code> implementation must implement
     *     the <code>java.io.Serializable</code> interface.</li>
     * <li>Any attempt to add a <code>null</code> key or value must
     *     throw a <code>NullPointerException</code>.</li>
     * <li>Any attempt to add a key that is not a String must throw
     *     a <code>ClassCastException</code>.</li>
     * <li>If the attribute name specified as a key matches a property
     *     of this {@link UIComponent}'s implementation class, the following
     *     methods will have special behavior:
     *     <ul>
     *     <li><code>containsKey</code> - Return <code>false</code>.</li>
     *     <li><code>get()</code> - If the property is readable, call
     *         the getter method and return the returned value (wrapping
     *         primitive values in their corresponding wrapper classes);
     *         otherwise throw <code>IllegalArgumentException</code>.</li>
     *     <li><code>put()</code> - If the property is writeable, call
     *         the setter method to set the corresponding value (unwrapping
     *         primitive values in their corresponding wrapper classes).
     *         If the property is not writeable, or an attempt is made to
     *         set a property of primitive type to <code>null</code>,
     *         throw <code>IllegalArgumentException</code>.</li>
     *     <li><code>remove</code> - Throw
     *         <code>IllegalArgumentException</code>.</li>
     *     </ul></li>
     * </ul>
     * 
     */
    public abstract Map<String, Object> getAttributes();
    
    
    // ---------------------------------------------------------------- Bindings


    /**
     *
     * <p>Call through to {@link #getValueExpression} and examine the
     * result.  If the result is an instance of the wrapper class
     * mandated in {@link #setValueBinding}, extract the
     * <code>ValueBinding</code> instance and return it.  Otherwise,
     * wrap the result in an implementation of
     * <code>ValueBinding</code>, and return it.</p>
     *
     * @param name Name of the attribute or property for which to retrieve a
     *  {@link ValueBinding}
     *
     * @throws NullPointerException if <code>name</code>
     *  is <code>null</code>
     *
     * @deprecated This has been replaced by {@link #getValueExpression}.
     */
    public abstract ValueBinding getValueBinding(String name);


    /**
     * <p>Wrap the argument <code>binding</code> in an implementation of
     * {@link ValueExpression} and call through to {@link
     * #setValueExpression}.</p>
     *
     * @param name Name of the attribute or property for which to set a
     *  {@link ValueBinding}
     * @param binding The {@link ValueBinding} to set, or <code>null</code>
     *  to remove any currently set {@link ValueBinding}
     *
     * @throws IllegalArgumentException if <code>name</code> is one of
     *  <code>id</code> or <code>parent</code>
     * @throws NullPointerException if <code>name</code>
     *  is <code>null</code>
     *
     * @deprecated This has been replaced by {@link #setValueExpression}.
     */
    public abstract void setValueBinding(String name, ValueBinding binding);

    // The set of ValueExpressions for this component, keyed by property
    // name This collection is lazily instantiated
    // The set of ValueExpressions for this component, keyed by property
    // name This collection is lazily instantiated
    @Deprecated
    protected Map<String,ValueExpression> bindings = null;

    /**
     * <p>Return the {@link ValueExpression} used to calculate the value for the
     * specified attribute or property name, if any.</p>
     *
     * <p>This method must be overridden and implemented for components that
     * comply with JSF 1.2 and later.</p>
     *
     * @since 1.2
     *
     * @param name Name of the attribute or property for which to retrieve a
     *  {@link ValueExpression}
     *
     * @throws NullPointerException if <code>name</code>
     *  is <code>null</code>
     *
     */
    public ValueExpression getValueExpression(String name) {

        if (name == null) {
            throw new NullPointerException();
        }

        Map<String,ValueExpression> map = (Map<String,ValueExpression>)
              getStateHelper().get(UIComponentBase.PropertyKeys.bindings);
        return ((map != null) ? map.get(name) : null);

    }

    /**
     * <p>Set the {@link ValueExpression} used to calculate the value
     * for the specified attribute or property name, if any.</p>
     *
     * <p>The implementation must call {@link
     * ValueExpression#isLiteralText} on the argument
     * <code>expression</code>.  If <code>isLiteralText()</code> returns
     * <code>true</code>, invoke {@link ValueExpression#getValue} on the
     * argument expression and pass the result as the <code>value</code>
     * parameter in a call to <code>this.{@link
     * #getAttributes()}.put(name, value)</code> where <code>name</code>
     * is the argument <code>name</code>.  If an exception is thrown as
     * a result of calling {@link ValueExpression#getValue}, wrap it in
     * a {@link javax.faces.FacesException} and re-throw it.  If
     * <code>isLiteralText()</code> returns <code>false</code>, simply
     * store the un-evaluated <code>expression</code> argument in the
     * collection of <code>ValueExpression</code>s under the key given
     * by the argument <code>name</code>.</p>
     *
     * <p>This method must be overridden and implemented for components that
     * comply with JSF 1.2 and later.</p>
     *
     * @since 1.2
     *
     * @param name Name of the attribute or property for which to set a
     *  {@link ValueExpression}
     * @param binding The {@link ValueExpression} to set, or <code>null</code>
     *  to remove any currently set {@link ValueExpression}
     *
     * @throws IllegalArgumentException if <code>name</code> is one of
     *  <code>id</code> or <code>parent</code>
     * @throws NullPointerException if <code>name</code>
     *  is <code>null</code>
     *
     */
    public void setValueExpression(String name, ValueExpression binding) {

        if (name == null) {
            throw new NullPointerException();
        } else if ("id".equals(name) || "parent".equals(name)) {
            throw new IllegalArgumentException();
        }

        if (binding != null) {
            if (!binding.isLiteralText()) {
                //if (bindings == null) {
                //    //noinspection CollectionWithoutInitialCapacity
                //    bindings = new HashMap<String, ValueExpression>();
                //}
                // add this binding name to the 'attributesThatAreSet' list
                //List<String> sProperties = (List<String>)
                //      getStateHelper().get(PropertyKeysPrivate.attributesThatAreSet);

                 List<String> sProperties =
                      (List<String>) getStateHelper().get(PropertyKeysPrivate.attributesThatAreSet);
                if (sProperties == null) {
                    getStateHelper().add(PropertyKeysPrivate.attributesThatAreSet, name);
                } else if (!sProperties.contains(name)) {
                    getStateHelper().add(PropertyKeysPrivate.attributesThatAreSet, name);
                }
                getStateHelper().put(UIComponentBase.PropertyKeys.bindings,
                                     name,
                                     binding);
                //bindings.put(name, binding);
            } else {
                ELContext context =
                    FacesContext.getCurrentInstance().getELContext();
                try {
                    getAttributes().put(name, binding.getValue(context));
                } catch (ELException ele) {
                    throw new FacesException(ele);
                }
            }
        } else {
            //if (bindings != null) {
                // remove this binding name from the 'attributesThatAreSet' list
//                List<String> sProperties = getAttributesThatAreSet(false);
//                if (sProperties != null) {
//                    sProperties.remove(name);
//                }
                getStateHelper().remove(PropertyKeysPrivate.attributesThatAreSet,
                                        name);
                getStateHelper().remove(UIComponentBase.PropertyKeys.bindings, name);
                //bindings.remove(name);
               // if (bindings.isEmpty()) {
               //     bindings = null;
               // }
            }
       // }

    }

    // -------------------------------------------------------------- Properties

    boolean initialState;

    /**
     * <p class="changed_added_2_0">An implementation of {@link
     * PartialStateHolder#markInitialState}, this method is called by
     * the runtime to indicate that the instance should start tracking
     * changes to its state.</p>
     * @since 2.0
     */
    public void markInitialState() {
        initialState = true;
    }


    /**
     * <p class="changed_added_2_0">An implementation of {@link
     * PartialStateHolder#initialStateMarked}, this method is called by
     * the runtime to test if the {@link
     * PartialStateHolder#markInitialState} method was called.</p>
     * @since 2.0
     */
    public boolean initialStateMarked() {
        return initialState;
    }


    /**
     * <p class="changed_added_2_0">An implementation of {@link
     * PartialStateHolder#clearInitialState}, this method is called by
     * the runtime to tell the instance to stop tracking state
     * changes.</p>
     * @since 2.0
     */
    public void clearInitialState() {
        initialState = false;
    }


    /**
     * <p class="changed_added_2_0">Return the {@link StateHelper}
     * instance used to help this component implement {@link
     * PartialStateHolder}.</p>
     * @since 2.0
     */
    protected StateHelper getStateHelper() {
        return getStateHelper(true);
    }


    /**
     * <p class="changed_added_2_0">Like {@link #getStateHelper()}, but
     * only create a state helper instance if the argument
     * <code>creat</code> is <code>true</code>.</p>
     * @param create if <code>true</code>, a new {@link StateHelper}
     * instance will be created if it does not exist already.  If
     * <code>false</code>, and there is no existing
     * <code>StateHelper</code> instance, one will not be created and
     * <code>null</code> will be returned.
     * @since 2.0
     */
    protected StateHelper getStateHelper(boolean create) {

        if (create && stateHelper == null) {
            stateHelper = new ComponentStateHelper(this);
        }
        return stateHelper;

    }


    private boolean isInView;


    /**
     * <p class="changed_added_2_0">Return <code>true</code> if this
     * component is within the view hierarchy otherwise
     * <code>false</code></code>
     *
     * @since 2.0
     */
    public boolean isInView() {
        return isInView;
    }


    /**
     * <p class="changed_added_2_0">Updates the status as to whether or
     * not this component is currently within the view hierarchy.
     * <strong>This method must never be called by developers; a {@link
     * UIComponent}'s internal implementation will call it as components
     * are added to or removed from a parent's child <code>List</code>
     * or facet <code>Map</code></strong>.</p>
     *
     * @param isInView flag indicating whether or not this component is within
     *  the view hierachy
     *
     * @since 2.0
     */
    public void setInView(boolean isInView) {
        this.isInView = isInView;
    }


    /**
     * <p class="changed_added_2_0">Enable EL to access the <code>clientId</code>
     * of a component.  This is particularly useful in combination with the 
     * <code>component</code> and <code>cc</code> implicit
     * objects.  A default implementation is provided that simply calls
     * {@link FacesContext#getCurrentInstance} and then calls through to
     * {@link #getClientId(FacesContext)}.</p>
     * 
     * @since 2.0
     */
    
    public String getClientId() {
        FacesContext context = FacesContext.getCurrentInstance();
        return getClientId(context);
    }


    /**
     * <p>Return a client-side identifier for this component, generating
     * one if necessary.  The associated {@link Renderer}, if any,
     * will be asked to convert the clientId to a form suitable for
     * transmission to the client.</p>
     *
     * <p>The return from this method must be the same value throughout
     * the lifetime of the instance, unless the <code>id</code> property
     * of the component is changed, or the component is placed in
     * a {@link NamingContainer} whose client ID changes (for example,
     * {@link UIData}).  However, even in these cases, consecutive
     * calls to this method must always return the same value.  The
     * implementation must follow these steps in determining the
     * clientId:</p>
     *
     * <p>Find the closest ancestor to <b>this</b> component in the view
     * hierarchy that implements <code>NamingContainer</code>.  Call
     * <code>getContainerClientId()</code> on it and save the result as
     * the <code>parentId</code> local variable.  Call {@link #getId} on
     * <b>this</b> component and save the result as the
     * <code>myId</code> local variable.  If <code>myId</code> is
     * <code>null</code>, call
     * <code>context.getViewRoot().createUniqueId()</code> and assign
     * the result to myId.  If <code>parentId</code> is
     * non-<code>null</code>, let <code>myId</code> equal <code>parentId
     * + {@link UINamingContainer#getSeparatorChar} + myId</code>.  Call
     * {@link Renderer#convertClientId}, passing <code>myId</code>, and
     * return the result.</p>
     *
     * @param context The {@link FacesContext} for the current request
     *
     * @throws NullPointerException if <code>context</code>
     *  is <code>null</code>
     */
    public abstract String getClientId(FacesContext context);

    /**
     * <p>Allow components that implement {@link NamingContainer} to
     * selectively disable prepending their clientId to their
     * descendent's clientIds by breaking the prepending logic into a
     * seperately callable method.  See {@link #getClientId} for usage.</p>
     *
     * <p>By default, this method will call through to {@link
     * #getClientId} and return the result.
     *
     * @since 1.2
     *
     *  @throws NullPointerException if <code>context</code> is
     *  <code>null</code>
     */
    public String getContainerClientId(FacesContext context) {
        if (context == null) {
            throw new NullPointerException();
        }
        return this.getClientId(context);
    }

    /**
     * <p>Return the identifier of the component family to which this
     * component belongs.  This identifier, in conjunction with the value
     * of the <code>rendererType</code> property, may be used to select
     * the appropriate {@link Renderer} for this component instance.</p>
     */
    public abstract String getFamily();


    /**
     * <p>Return the component identifier of this {@link UIComponent}.</p>
     */
    public abstract String getId();


    /**
     * <p>Set the component identifier of this {@link UIComponent} (if any).
     * Component identifiers must obey the following syntax restrictions:</p>
     * <ul>
     * <li>Must not be a zero-length String.</li>
     * <li>First character must be a letter or an underscore ('_').</li>
     * <li>Subsequent characters must be a letter, a digit,
     *     an underscore ('_'), or a dash ('-').</li>
     * <li>
     * </ul>
     *
     * <p>Component identifiers must also obey the following semantic
     * restrictions (note that this restriction is <strong>NOT</strong>
     * enforced by the <code>setId()</code> implementation):</p>
     * <ul>
     * <li>The specified identifier must be unique among all the components
     *     (including facets) that are descendents of the nearest ancestor
     *     {@link UIComponent} that is a {@link NamingContainer}, or within
     *     the scope of the entire component tree if there is no such
     *     ancestor that is a {@link NamingContainer}.</li>
     * </ul>
     *
     * @param id The new component identifier, or <code>null</code> to indicate
     *  that this {@link UIComponent} does not have a component identifier
     *
     * @throws IllegalArgumentException if <code>id</code> is not
     *  syntactically valid
     */
    public abstract void setId(String id);


    /**
     * <p>Return the parent {@link UIComponent} of this
     * <code>UIComponent</code>, if any.  A component must allow child
     * components to be added to and removed from the list of children
     * of this component, even though the child component returns null
     * from <code>getParent( )</code>.</p>
     */
    public abstract UIComponent getParent();


    /**
     * <p class="changed_modified_2_0">Set the parent
     * <code>UIComponent</code> of this <code>UIComponent</code>.  <span
     * class="changed_added_2_0">This method will cause an {@link
     * javax.faces.event.PostAddToViewEvent} to be published and if
     * <code>parent.isInView()</code> returns <code>true</code> an
     * {@link javax.faces.event.PostAddToViewEvent} will be published as
     * well.  <strong>This method must never be called by developers; a
     * {@link UIComponent}'s internal implementation will call it as
     * components are added to or removed from a parent's child
     * <code>List</code> or facet <code>Map</code></strong></span>.</p>
     *
     * @param parent The new parent, or <code>null</code> for the root node
     *  of a component tree
     */
    public abstract void setParent(UIComponent parent);


    /**
     * <p>Return <code>true</code> if this component (and its children)
     * should be rendered during the <em>Render Response</em> phase
     * of the request processing lifecycle.</p>
     */
    public abstract boolean isRendered();


    /**
     * <p>Set the <code>rendered</code> property of this
     * {@link UIComponent}.</p>
     *
     * @param rendered If <code>true</code> render this component;
     *  otherwise, do not render this component
     */
    public abstract void setRendered(boolean rendered);


    /**
     * <p>Return the {@link Renderer} type for this {@link UIComponent}
     * (if any).</p>
     */
    public abstract String getRendererType();


    /**
     * <p>Set the {@link Renderer} type for this {@link UIComponent},
     * or <code>null</code> for components that render themselves.</p>
     *
     * @param rendererType Logical identifier of the type of
     *  {@link Renderer} to use, or <code>null</code> for components
     *  that render themselves
     */
    public abstract void setRendererType(String rendererType);


    /**
     * <p>Return a flag indicating whether this component is responsible
     * for rendering its child components.  The default implementation
     * in {@link UIComponentBase#getRendersChildren} tries to find the
     * renderer for this component.  If it does, it calls {@link
     * Renderer#getRendersChildren} and returns the result.  If it
     * doesn't, it returns false.  As of version 1.2 of the JavaServer
     * Faces Specification, component authors are encouraged to return
     * <code>true</code> from this method and rely on {@link
     * UIComponentBase#encodeChildren}.</p>
     */
    public abstract boolean getRendersChildren();
    

    
    private Map<String, String> resourceBundleMap = null;
    
    /**
     * <p class="changed_added_2_0">Return a
     * <code>Map&lt;String,String&gt;</code> of the
     * <code>ResourceBundle</code> for this component.  A component may
     * have a <code>ResourceBundle</code> associated with it.  This
     * bundle may contain localized properties relating to instances of
     * this component.  The default implementation first looks for a
     * <code>ResourceBundle</code> with a base name equal to the fully
     * qualified class name of the current <code>UIComponent this</code>
     * and <code>Locale</code> equal to the <code>Locale</code> of the
     * current <code>UIViewRoot</code>.  If no such bundle is found, and
     * the component is a composite component, let <em>resourceName</em>
     * be the <em>resourceName</em> of the {@link Resource} for this
     * composite component, replacing the file extension with
     * ".properties".  Let <em>libraryName</em> be the
     * <em>libraryName</em> of the the {@link Resource} for this
     * composite component.  Call {@link
     * javax.faces.application.ResourceHandler#createResource(java.lang.String,java.lang.String)},
     * passing the derived <em>resourceName</em> and
     * <em>libraryName</em>.  Note that this will automatically allow
     * for the localization of the <code>ResourceBundle</code> due to
     * the localization facility implemented in
     * <code>createResource</code>, which is specified in section
     * JSF.2.6.1.3 of the spec prose document.  If the resultant {@link
     * Resource} exists and can be found, the <code>InputStream</code>
     * for the resource is used to create a <code>ResourceBundle</code>.
     * If either of the two previous steps for obtaining the
     * <code>ResourceBundle</code> for this component is successful, the
     * <code>ResourceBundle</code> is wrapped in a
     * <code>Map&lt;String,String&gt;</code> and returned.  Otherwise
     * <code>Collections.EMPTY_MAP</code> is returned.</p>
     *
     * @since 2.0
     */
    public Map<String,String> getResourceBundleMap() {
        
        if (null == resourceBundleMap) {
            // See if there is a ResourceBundle under the FQCN for this class
            String className = this.getClass().getName();
            Locale currentLocale = null;
            FacesContext context = null;
            UIViewRoot root = null;
            ResourceBundle resourceBundle = null;
            
            // Step 1: look for a ResourceBundle under the FQCN of this instance
            if (null != (context = FacesContext.getCurrentInstance())) {
                if (null != (root = context.getViewRoot())) {
                    currentLocale = root.getLocale();
                }
            }
            if (null == currentLocale) {
                currentLocale = Locale.getDefault();
            }
            try {
                resourceBundle = 
                        ResourceBundle.getBundle(className, currentLocale);
            } catch (MissingResourceException e) {
                // It is not an error if there is no ResourceBundle
            }
            
            // Step 2: if this is a composite component, look for a 
            // ResourceBundle as a Resource
            if (null == resourceBundle) {
                if (this.getAttributes().containsKey(Resource.COMPONENT_RESOURCE_KEY)) {
                    Resource ccResource = (Resource)
                            this.getAttributes().get(Resource.COMPONENT_RESOURCE_KEY);
                    if (null != ccResource) {
                        if (null != (ccResource = 
                                findComponentResourceBundleLocaleMatch(context, 
                                ccResource.getResourceName(), 
                                ccResource.getLibraryName()))) {
                            try {
                                InputStream propertiesInputStream = ccResource.getInputStream();
                                resourceBundle = new PropertyResourceBundle(propertiesInputStream);
                            } catch (IOException ex) {
                                Logger.getLogger(UIComponent.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                }
            }
            
            // Step 3: if the previous steps yielded a ResourceBundle, wrap it
            // with a Map
            
            if (null != resourceBundle) {
                final ResourceBundle bundle = resourceBundle;
                resourceBundleMap = 
                        new Map() {
                            // this is an immutable Map

                            public String toString() {
                                StringBuffer sb = new StringBuffer();
                                Iterator<Map.Entry<String, Object>> entries =
                                        this.entrySet().iterator();
                                Map.Entry<String, Object> cur;
                                while (entries.hasNext()) {
                                    cur = entries.next();
                                    sb.append(cur.getKey()).append(": ").append(cur.getValue()).append('\n');
                                }

                                return sb.toString();
                            }

                            // Do not need to implement for immutable Map
                            public void clear() {
                                throw new UnsupportedOperationException();
                            }


                            public boolean containsKey(Object key) {
                                boolean result = false;
                                if (null != key) {
                                    result = (null != bundle.getObject(key.toString()));
                                }
                                return result;
                            }


                            public boolean containsValue(Object value) {
                                Enumeration<String> keys = bundle.getKeys();
                                boolean result = false;
                                while (keys.hasMoreElements()) {
                                    Object curObj = bundle.getObject(keys.nextElement());
                                    if ((curObj == value) ||
                                            ((null != curObj) && curObj.equals(value))) {
                                        result = true;
                                        break;
                                    }
                                }
                                return result;
                            }


                            public Set<Map.Entry<String, Object>> entrySet() {
                                HashMap<String, Object> mappings = new HashMap<String, Object>();
                                Enumeration<String> keys = bundle.getKeys();
                                while (keys.hasMoreElements()) {
                                    String key = keys.nextElement();
                                    Object value = bundle.getObject(key);
                                    mappings.put(key, value);
                                }
                                return mappings.entrySet();
                            }


                            @Override
                            public boolean equals(Object obj) {
                                return !((obj == null) || !(obj instanceof Map))
                                         && entrySet().equals(((Map) obj).entrySet());

                            }


                            public Object get(Object key) {
                                if (null == key) {
                                    return null;
                                }
                                try {
                                    return bundle.getObject(key.toString());
                                } catch (MissingResourceException e) {
                                    return "???" + key + "???";
                                }
                            }


                            public int hashCode() {
                                return bundle.hashCode();
                            }


                            public boolean isEmpty() {
                                Enumeration<String> keys = bundle.getKeys();
                                return !keys.hasMoreElements();
                            }


                            public Set keySet() {
                                Set<String> keySet = new HashSet<String>();
                                Enumeration<String> keys = bundle.getKeys();
                                while (keys.hasMoreElements()) {
                                    keySet.add(keys.nextElement());
                                }
                                return keySet;
                            }


                            // Do not need to implement for immutable Map
                            public Object put(Object k, Object v) {
                                throw new UnsupportedOperationException();
                            }


                            // Do not need to implement for immutable Map
                            public void putAll(Map t) {
                                throw new UnsupportedOperationException();
                            }


                            // Do not need to implement for immutable Map
                            public Object remove(Object k) {
                                throw new UnsupportedOperationException();
                            }


                            public int size() {
                                int result = 0;
                                Enumeration<String> keys = bundle.getKeys();
                                while (keys.hasMoreElements()) {
                                    keys.nextElement();
                                    result++;
                                }
                                return result;
                            }


                            public java.util.Collection values() {
                                ArrayList<Object> result = new ArrayList<Object>();
                                Enumeration<String> keys = bundle.getKeys();
                                while (keys.hasMoreElements()) {
                                    result.add(
                                            bundle.getObject(keys.nextElement()));
                                }
                                return result;
                            }
                        };

            }

            if (null == resourceBundleMap) {
                resourceBundleMap = Collections.EMPTY_MAP;
            }

        }
        
        return resourceBundleMap;
    }

    // PENDING(rlubke): I'm sure there's a more efficient
    // way to handle this.
    private Resource findComponentResourceBundleLocaleMatch(FacesContext context, 
            String resourceName, String libraryName) {
        Resource result = null;
        ResourceBundle resourceBundle = null;
        int i;
        if (-1 != (i = resourceName.lastIndexOf("."))) {
            resourceName = resourceName.substring(0, i) +
                    ".properties";
            if (null != context) {
                result = context.getApplication().getResourceHandler().
                        createResource(resourceName, libraryName);
                try {
                    InputStream propertiesInputStream = result.getInputStream();
                    resourceBundle = new PropertyResourceBundle(propertiesInputStream);
                } catch (IOException ex) {
                    Logger.getLogger(UIComponent.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        result = (null != resourceBundle) ? result : null;
        
        return result;
    }
    
    
    // This is necessary for JSF components that extend from UIComponent
    // directly rather than extending from UIComponentBase.  Such components
    // may need to have implementations provided for methods that originated
    // from a spec version more recent than the version with which the component
    // complies.  Currently this private property is only consulted in the
    // getValueExpression() method.
    private boolean isUIComponentBase;
    private boolean isUIComponentBaseIsSet = false;

    private boolean isUIComponentBase() {
        if (!isUIComponentBaseIsSet) {
            isUIComponentBase = (this instanceof UIComponentBase);
        }

        return isUIComponentBase;
    }


    // ------------------------------------------------- Tree Management Methods


    /**
     * <p><span class="changed_modified_2_0">Return</span> a mutable
     * <code>List</code> representing the child {@link UIComponent}s
     * associated with this component.  The returned implementation must
     * support all of the standard and optional <code>List</code>
     * methods, plus support the following additional requirements:</p>
     * <ul> <li>The <code>List</code> implementation must implement the
     * <code>java.io.Serializable</code> interface.</li> <li>Any attempt
     * to add a <code>null</code> must throw a NullPointerException</li>
     * <li>Any attempt to add an object that does not implement {@link
     * UIComponent} must throw a ClassCastException.</li> <li>Whenever a
     * new child component is added, the <code>parent</code> property of
     * the child must be set to this component instance.  If the
     * <code>parent</code> property of the child was already non-null,
     * the child must first be removed from its previous parent (where
     * it may have been either a child or a facet).</li> <li>Whenever an
     * existing child component is removed, the <code>parent</code>
     * property of the child must be set to <code>null</code>.</li>

     * <li class="changed_added_2_0"><p>After the child component has
     *     been added to the view, if the following condition is
     *     <strong>not</strong> met:</p>
     *
     *     <ul><p>{@link javax.faces.context.FacesContext#isPostback}
     *     returns <code>true</code> and {@link
     *     javax.faces.context.FacesContext#getCurrentPhaseId} returns {@link
     *     javax.faces.event.PhaseId#RESTORE_VIEW}</p></ul>

     *     <p>{@link javax.faces.application.Application#publishEvent}
     *     must be called, passing {@link
     *     javax.faces.event.PostAddToViewEvent}<code>.class</code>
     *     as the first argument and the newly added component as the
     *     second argument.</p>

     * </li>

     * </ul>
     */
    public abstract List<UIComponent> getChildren();


    /**
     * <p>Return the number of child {@link UIComponent}s that are
     * associated with this {@link UIComponent}.  If there are no
     * children, this method must return 0.  The method must not cause
     * the creation of a child component list.</p>
     */
    public abstract int getChildCount();


    /**
     * <p>Search for and return the {@link UIComponent} with an <code>id</code>
     * that matches the specified search expression (if any), according to the
     * algorithm described below.</p>
     *
     * <p>For a method to find a component given a simple
     * <code>clientId</code>, see {@link #invokeOnComponent}.</p>
     *
     * <p>Component identifiers are required to be unique within the scope of
     * the closest ancestor {@link NamingContainer} that encloses this
     * component (which might be this component itself).  If there are no
     * {@link NamingContainer} components in the ancestry of this component,
     * the root component in the tree is treated as if it were a
     * {@link NamingContainer}, whether or not its class actually implements
     * the {@link NamingContainer} interface.</p>
     *
     * <p>A <em>search expression</em> consists of either an identifier
     * (which is matched exactly against the <code>id</code> property of
     * a {@link UIComponent}, or a series of such identifiers linked by
     * the {@link UINamingContainer#getSeparatorChar} character value.
     * The search algorithm should operates as follows, though alternate
     * alogrithms may be used as long as the end result is the same:</p>

     * <ul>
     * <li>Identify the {@link UIComponent} that will be the base for searching,
     *     by stopping as soon as one of the following conditions is met:
     *     <ul>
     *     <li>If the search expression begins with the the separator character
     *         (called an "absolute" search expression),
     *         the base will be the root {@link UIComponent} of the component
     *         tree.  The leading separator character will be stripped off,
     *         and the remainder of the search expression will be treated as
     *         a "relative" search expression as described below.</li>
     *     <li>Otherwise, if this {@link UIComponent} is a
     *         {@link NamingContainer} it will serve as the basis.</li>
     *     <li>Otherwise, search up the parents of this component.  If
     *         a {@link NamingContainer} is encountered, it will be the base.
     *         </li>
     *     <li>Otherwise (if no {@link NamingContainer} is encountered)
     *         the root {@link UIComponent} will be the base.</li>
     *     </ul></li>
     * <li>The search expression (possibly modified in the previous step) is now
     *     a "relative" search expression that will be used to locate the
     *     component (if any) that has an <code>id</code> that matches, within
     *     the scope of the base component.  The match is performed as follows:
     *     <ul>
     *     <li>If the search expression is a simple identifier, this value is
     *         compared to the <code>id</code> property, and then recursively
     *         through the facets and children of the base {@link UIComponent}
     *         (except that if a descendant {@link NamingContainer} is found,
     *         its own facets and children are not searched).</li>
     *     <li>If the search expression includes more than one identifier
     *         separated by the separator character, the first identifier is
     *         used to locate a {@link NamingContainer} by the rules in the
     *         previous bullet point.  Then, the <code>findComponent()</code>
     *         method of this {@link NamingContainer} will be called, passing
     *         the remainder of the search expression.</li>
     *     </ul></li>
     * </ul>
     *
     * @param expr Search expression identifying the {@link UIComponent}
     *  to be returned
     *
     * @return the found {@link UIComponent}, or <code>null</code>
     *  if the component was not found.
     *
     * @throws IllegalArgumentException if an intermediate identifier
     *  in a search expression identifies a {@link UIComponent} that is
     *  not a {@link NamingContainer}
     * @throws NullPointerException if <code>expr</code>
     *  is <code>null</code>
     */
    public abstract UIComponent findComponent(String expr);

    /**
     * <p>Starting at this component in the View hierarchy, search for a
     * component with a <code>clientId</code> equal to the argument
     * <code>clientId</code> and, if found, call the {@link
     * ContextCallback#invokeContextCallback} method on the argument
     * <code>callback</code>, passing the current {@link FacesContext}
     * and the found component as arguments. This method is similar to
     * {@link #findComponent} but it does not support the leading {@link
     * UINamingContainer#getSeparatorChar} syntax for searching from the
     * root of the View.</p>
     *
     * <p>The default implementation will first check if
     * <code>this.getClientId()</code> is equal to the argument
     * <code>clientId</code>.  If so, call the {@link
     * ContextCallback#invokeContextCallback} method on the argument callback,
     * passing through the <code>FacesContext</code> argument and
     * passing this as the component argument.  If an
     * <code>Exception</code> is thrown by the callback, wrap it in a
     * {@link FacesException} and re-throw it.  Otherwise, return
     * <code>true</code>.</p>
     *
     * <p>Otherwise, for each component returned by {@link
     * #getFacetsAndChildren}, call <code>invokeOnComponent()</code>
     * passing the arguments to this method, in order.  The first time
     * <code>invokeOnComponent()</code> returns true, abort traversing
     * the rest of the <code>Iterator</code> and return
     * <code>true</code>.</p>
     *
     * <p>When calling {@link ContextCallback#invokeContextCallback}
     * the implementation of this method must guarantee that the state
     * of the component passed to the callback correctly reflects the
     * component's position in the View hierarchy with respect to any
     * state found in the argument <code>clientId</code>.  For example,
     * an iterating component such as {@link UIData} will need to set
     * its row index to correctly reflect the argument
     * <code>clientId</code> before finding the appropriate child
     * component backed by the correct row.  When the callback returns,
     * either normally or by throwing an <code>Exception</code> the
     * implementation of this method must restore the state of the view
     * to the way it was before invoking the callback.</p>
     *
     * <p>If none of the elements from {@link
     * #getFacetsAndChildren} returned <code>true</code> from
     * <code>invokeOnComponent()</code>, return <code>false</code>.</p>
     *
     * <p>Simple usage example to find a component by
     * <code>clientId</code>.</p>

* <pre><code>
private UIComponent found = null;

private void doFind(FacesContext context, String clientId) {
  context.getViewRoot().invokeOnComponent(context, clientId,
      new ContextCallback() {
         public void invokeContextCallback(FacesContext context,
                                       UIComponent component) {
           found = component;
         }
      });
}
* </code></pre>

     *
     *
     * @since 1.2
     *
     * @param context the {@link FacesContext} for the current request
     *
     * @param clientId the client identifier of the component to be passed
     * to the argument callback.
     *
     * @param callback an implementation of the Callback interface.
     *
     * @throws NullPointerException if any of the arguments are null
     *
     * @throws FacesException if the argument Callback throws an
     * Exception, it is wrapped in a <code>FacesException</code> and re-thrown.
     *
     * @return <code>true</code> if the a component with the given
     * <code>clientId</code> is found, the callback method was
     * successfully invoked passing that component as an argument, and
     * no Exception was thrown.  Returns <code>false</code> if no
     * component with the given <code>clientId</code> is found.
     *
     */

    public boolean invokeOnComponent(FacesContext context, String clientId,
            ContextCallback callback) throws FacesException {
        if (null == context || null == clientId || null == callback) {
            throw new NullPointerException();
        }

        boolean found = false;
        if (clientId.equals(this.getClientId(context))) {
            try {
                callback.invokeContextCallback(context, this);
                return true;
            } catch (Exception e) {
                throw new FacesException(e);
            }
        } else {
            Iterator<UIComponent> itr = this.getFacetsAndChildren();

            while (itr.hasNext() && !found) {
                found = itr.next().invokeOnComponent(context, clientId,
                        callback);
            }
        }
        return found;
    }

    // ------------------------------------------------ Facet Management Methods


    /**
     * <p>Return a mutable <code>Map</code> representing the facet
     * {@link UIComponent}s associated with this {@link UIComponent},
     * keyed by facet name (which must be a String).  The returned
     * implementation must support all of the standard and optional
     * <code>Map</code> methods, plus support the following additional
     * requirements:</p>

     * <ul>
     * <li>The <code>Map</code> implementation must implement
     *     the <code>java.io.Serializable</code> interface.</li>
     * <li>Any attempt to add a <code>null</code> key or value must
     *     throw a NullPointerException.</li>
     * <li>Any attempt to add a key that is not a String must throw
     *     a ClassCastException.</li>
     * <li>Any attempt to add a value that is not a {@link UIComponent}
     *     must throw a ClassCastException.</li>
     * <li>Whenever a new facet {@link UIComponent} is added:
     *     <ul>
     *     <li>The <code>parent</code> property of the component must be set to
     *         this component instance.</li>
     *     <li>If the <code>parent</code> property of the component was already
     *     non-null, the component must first be removed from its previous
     *     parent (where it may have been either a child or a facet).</li>
     *     </ul></li>

     * <li>Whenever an existing facet {@link UIComponent} is removed:
     *     <ul>
     *     <li>The <code>parent</code> property of the facet must be
     *         set to <code>null</code>.</li>
     *     </ul></li>
     * </ul>
     */
    public abstract Map<String, UIComponent> getFacets();

    /**
     * <p>Return the number of facet {@link UIComponent}s that are
     * associated with this {@link UIComponent}.  If there are no
     * facets, this method must return 0.  The method must not cause
     * the creation of a facet component map.</p>
     *
     * <p>For backwards compatability with classes that extend UIComponent
     * directly, a default implementation is provided that simply calls
     * {@link #getFacets} and then calls the <code>size()</code> method on the
     * returned <code>Map</code>.  A more optimized version of this method is
     * provided in {@link UIComponentBase#getFacetCount}.
     *
     * @since 1.2
     */
    public int getFacetCount() {
        return (getFacets().size());
    }



    /**
     * <p>Convenience method to return the named facet, if it exists, or
     * <code>null</code> otherwise.  If the requested facet does not
     * exist, the facets Map must not be created.</p>
     *
     * @param name Name of the desired facet
     */
    public abstract UIComponent getFacet(String name);


    /**
     * <p>Return an <code>Iterator</code> over the facet followed by child
     * {@link UIComponent}s of this {@link UIComponent}.
     * Facets are returned in an undefined order, followed by
     * all the children in the order they are stored in the child list. If this
     * component has no facets or children, an empty <code>Iterator</code>
     * is returned.</p>
     *
     * <p>The returned <code>Iterator</code> must not support the
     * <code>remove()</code> operation.</p>
     */
    public abstract Iterator<UIComponent> getFacetsAndChildren();


    // -------------------------------------------- Lifecycle Processing Methods


    /**
     * <p>Broadcast the specified {@link FacesEvent} to all registered
     * event listeners who have expressed an interest in events of this
     * type.  Listeners are called in the order in which they were
     * added.</p>  
     * <p class="changed_added_2_0">If the <code>event</code> is an instance of 
     * {@link javax.faces.event.BehaviorEvent} and the current 
     * <code>component</code> is the source of the <code>event</code>
     * call {@link javax.faces.event.BehaviorEvent#getBehavior} to get the
     * {@link javax.faces.component.behavior.Behavior} for the event.  If the
     * behavior implements {@link javax.faces.component.behavior.ClientBehavior},
     * call {@link javax.faces.component.behavior.ClientBehavior#broadcast(javax.faces.event.BehaviorEvent)}}.</p>
     *
     * @param event The {@link FacesEvent} to be broadcast
     *
     * @throws AbortProcessingException Signal the JavaServer Faces
     *  implementation that no further processing on the current event
     *  should be performed
     * @throws IllegalArgumentException if the implementation class
     *  of this {@link FacesEvent} is not supported by this component
     * @throws NullPointerException if <code>event</code> is
     * <code>null</code>
     */
    public abstract void broadcast(FacesEvent event)
        throws AbortProcessingException;


    /**
     * <p>Decode any new state of this {@link UIComponent} from the
     * request contained in the specified {@link FacesContext}, and store
     * this state as needed.</p>
     * <p>During decoding, events may be enqueued for later processing
     * (by event listeners who have registered an interest),  by calling
     * <code>queueEvent()</code>.</p>
     *
     * @param context {@link FacesContext} for the request we are processing
     *
     * @throws NullPointerException if <code>context</code>
     *  is <code>null</code>
     */
    public abstract void decode(FacesContext context);
    
    /**
     * <p class="changed_added_2_0">Perform a tree visit starting at
     * this node in the tree.</p>
     *
     * <div class="changed_added_2_0">
     *
     * <p>UIComponent.visitTree() implementations do not invoke the
     * {@link VisitCallback} directly, but instead call {@link
     * VisitContext#invokeVisitCallback} to invoke the callback.  This
     * allows {@code VisitContext} implementations to provide optimized
     * tree traversals, for example by only calling the {@code
     * VisitCallback} for a subset of components.</p>
     *
     * <p>UIComponent.visitTree() implementations must call
     * UIComponent.pushComponentToEL() before performing the
     * visit and UIComponent.popComponentFromEL() after the
     * visit.</p>
     *
     * @param context the <code>VisitContext</code> for this visit
     * @param callback the <code>VisitCallback</code> instance
     * whose <code>visit</code> method will be called
     * for each node visited.
     * @return component implementations may return <code>true</code> 
     *   to indicate that the tree visit is complete (eg. all components
     *   that need to be visited have been visited).  This results in
     *   the tree visit being short-circuited such that no more components
     *   are visited.
     *
     * </div>
     *
     * @see VisitContext#invokeVisitCallback VisitContext.invokeVisitCallback()
     *
     * @since 2.0
     */
    public boolean visitTree(VisitContext context, 
                             VisitCallback callback) {

        // First check to see whether we are visitable.  If not
        // short-circuit out of this subtree, though allow the
        // visit to proceed through to other subtrees.
        if (!isVisitable(context))
            return false;

        // Push ourselves to EL before visiting
        FacesContext facesContext = context.getFacesContext();
        pushComponentToEL(facesContext, null);

        try {
            // Visit ourselves.  Note that we delegate to the 
            // VisitContext to actually perform the visit.
            VisitResult result = context.invokeVisitCallback(this, callback);

            // If the visit is complete, short-circuit out and end the visit
            if (result == VisitResult.COMPLETE)
              return true;

            // Visit children if necessary
            if (result == VisitResult.ACCEPT) {
                Iterator<UIComponent> kids = this.getFacetsAndChildren();

                while(kids.hasNext()) {
                    boolean done = kids.next().visitTree(context, callback);

                    // If any kid visit returns true, we are done.
                    if (done)
                        return true;
                }
            }
        }
        finally {
            // Pop ourselves off the EL stack
            popComponentFromEL(facesContext);
        }

        // Return false to allow the visit to continue
        return false;
    }

    /**
     * <p class="changed_added_2_0">Return <code>true</code> if this
     * component should be visited, <code>false</code> otherwise.
     * Called by {@link UIComponent#visitTree UIComponent.visitTree()}
     * to determine whether this component satisfies the hints returned
     * by {@link javax.faces.component.visit.VisitContext#getHints}.</p>

     * <div class="changed_added_2_0">

     * <p>If this method returns false, the tree visited is
     * short-circuited such that neither the component nor any of its
     * descendents will be visited></p> 

     * <p>Custom {@code visitTree()} implementations may call this
     * method to determine whether the component is visitable before
     * performing any visit-related processing.</p>
     *
     * </div>
     *
     * @since 2.0
     */
    protected boolean isVisitable(VisitContext context) {

        // VisitHints currently defines two hints that affect 
        // visitability: VIIST_RENDERED and VISIT_TRANSIENT.
        // Check for both of these and if set, verify that 
        // we comply.
        Set<VisitHint> hints = context.getHints();

        if ((hints.contains(VisitHint.SKIP_UNRENDERED) && 
                !this.isRendered())                    ||
            (hints.contains(VisitHint.SKIP_TRANSIENT)  && 
                this.isTransient())) {
            return false;
        }

        return true;
    }

    /**
     * <p><span class="changed_modified_2_0">If</span> our
     * <code>rendered</code> property is <code>true</code>, render the
     * beginning of the current state of this {@link UIComponent} to the
     * response contained in the specified {@link FacesContext}. 
     * Call {@link #pushComponentToEL(javax.faces.context.FacesContext,javax.faces.component.UIComponent)}.
     * Call {@link javax.faces.application.Application#publishEvent}, passing
     * {@link javax.faces.event.PreRenderComponentEvent}<code>.class</code> as the
     * first argument and the component instance to be rendered as the
     * second argument.</p></li>

     * <p>If a {@link Renderer} is associated with this {@link
     * UIComponent}, the actual encoding will be delegated to
     * {@link Renderer#encodeBegin(FacesContext, UIComponent)}.
     * </p>
     *
     * <p class="changed_added_2_0">If our <code>rendered</code> property is
     * <code>false</code>, call {@link #pushComponentToEL(javax.faces.context.FacesContext,javax.faces.component.UIComponent)}
     * and return immediately.</p>
     *
     * @param context {@link FacesContext} for the response we are creating
     *
     * @throws IOException if an input/output error occurs while rendering
     * @throws NullPointerException if <code>context</code>
     *  is <code>null</code>
     */
    public abstract void encodeBegin(FacesContext context) throws IOException;


    /**
     * <p>If our <code>rendered</code> property is <code>true</code>,
     * render the child {@link UIComponent}s of this {@link UIComponent}.
     * This method will only be called
     * if the <code>rendersChildren</code> property is <code>true</code>.</p>
     *
     * <p>If a {@link Renderer} is associated with this {@link UIComponent},
     * the actual encoding will be delegated to
     * {@link Renderer#encodeChildren(FacesContext, UIComponent)}.
     * <span class="changed_modified_2_0">If no {@link Renderer} is associated
     * with this {@link UIComponent}, iterate over each of the children of this
     * component and call 
     * {@link #encodeAll(javax.faces.context.FacesContext)}.</span></p>
     *
     * @param context {@link FacesContext} for the response we are creating
     *
     * @throws IOException if an input/output error occurs while rendering
     * @throws NullPointerException if <code>context</code>
     *  is <code>null</code>
     */
    public abstract void encodeChildren(FacesContext context) throws IOException;


    /**
     * <p><span class="changed_modified_2_0">If</span> our
     * <code>rendered</code> property is <code>true</code>, render the
     * ending of the current state of this {@link UIComponent}.</p>
     *
     * <p>If a {@link Renderer} is associated with this {@link UIComponent},
     * the actual encoding will be delegated to
     * {@link Renderer#encodeEnd(FacesContext, UIComponent)}.</p>
     *
     * <p class="changed_added_2_0">Call {@link
     * UIComponent#popComponentFromEL}. before returning regardless of the value
     *  of the <code>rendered</code> property.</p>
     *
     * @param context {@link FacesContext} for the response we are creating
     *
     * @throws IOException if an input/output error occurs while rendering
     * @throws NullPointerException if <code>context</code>
     *  is <code>null</code>
     */
    public abstract void encodeEnd(FacesContext context) throws IOException;

    /**
     * <p>If this component
     * returns <code>true</code> from {@link #isRendered}, take the
     * following action.</p>
     *
     * <p>Render this component and all its children that return
     * <code>true</code> from <code>isRendered()</code>, regardless of
     * the value of the {@link #getRendersChildren} flag.</p></li>

     * @since 1.2
     *
     * @throws IOException if an input/output error occurs while rendering
     * @throws NullPointerException if <code>context</code>
     *  is <code>null</code>
     */
    public void encodeAll(FacesContext context) throws IOException {

        if (context == null) {
            throw new NullPointerException();
        }

        if (!isRendered()) {
            return;
        }

        encodeBegin(context);
        if (getRendersChildren()) {
            encodeChildren(context);
        } else if (this.getChildCount() > 0) {
            for (UIComponent kid : getChildren()) {
                kid.encodeAll(context);
            }
        }

        encodeEnd(context);

    }


    private UIComponent previouslyPushed = null;
    private UIComponent previouslyPushedCompositeComponent = null;
    private boolean pushed;

    /**
     * <p class="changed_added_2_0">Push the current
     * <code>UIComponent</code> <code>this</code> to the {@link FacesContext}
     * attribute map using the key {@link #CURRENT_COMPONENT} saving the previous
     * <code>UIComponent</code> associated with {@link #CURRENT_COMPONENT} for a
     * subsequent call to {@link #popComponentFromEL}.</p>
     *
     * <pclass="changed_added_2_0">This method and <code>popComponentFromEL()</code> form the basis for
     * the contract that enables the EL Expression "<code>#{component}</code>" to
     * resolve to the "current" component that is being processed in the
     * lifecycle.  The requirements for when <code>pushComponentToEL()</code> and
     * <code>popComponentFromEL()</code> must be called are specified as
     * needed in the javadoc for this class.</p>
     *
     * <p class="changed_added_2_0">After
     * <code>pushComponentToEL()</code> returns, a call to {@link
     * #getCurrentComponent} must return <code>this</code>
     * <code>UIComponent</code> instance until
     * <code>popComponentFromEL()</code> is called, after which point
     * the previous <code>UIComponent</code> instance will be returned
     * from <code>getCurrentComponent()</code></p>
     *
     * @param context the {@link FacesContext} for the current request
     * @param component the <code>component</code> to push to the EL.  If
     *  <code>component</code> is <code>null</code> the <code>UIComponent</code>
     *  instance that this call was invoked upon will be pushed to the EL.
     *
     * @throws NullPointerException if <code>context</code> is <code>null</code>
     *
     * @see javax.faces.context.FacesContext#getAttributes()
     *
     * @since 2.0
     */
    public final void pushComponentToEL(FacesContext context, UIComponent component) {

        if (context == null) {
            throw new NullPointerException();
        }

        Map<Object,Object> contextMap = context.getAttributes();
        if (null == component) {
            component = this;
        }
        if (contextMap != null) {
            pushed = true;
            previouslyPushed = (UIComponent) contextMap.put(CURRENT_COMPONENT, component);
            // If this is a composite component...
            if (UIComponent.isCompositeComponent(component)) {
                // make it so #{cc} resolves to this composite 
                // component, preserving the previous value if present
                previouslyPushedCompositeComponent = 
                        (UIComponent) contextMap.put(CURRENT_COMPOSITE_COMPONENT, component);
            }
        }

    }


    /**
     * <p class="changed_added_2_0">Pop the current
     * <code>UIComponent</code> from the {@link FacesContext} attributes map
     * so that the previous <code>UIComponent</code>, if any, becomes the current
     * component.</p>
     *
     * @param context the {@link FacesContext} for the current request
     * 
     * @throws NullPointerException if <code>context</code> is <code>null</code>
     *
     * @see javax.faces.context.FacesContext#getAttributes()
     *
     * @since 2.0
     */
    public final void popComponentFromEL(FacesContext context) {

        if (context == null) {
            throw new NullPointerException();
        }

        Map<Object,Object> contextMap = context.getAttributes();
        if (contextMap != null) {

            if (!pushed) {
                return;
            }
            UIComponent c;
            pushed = false;
            if (previouslyPushed != null) {
                c = (UIComponent) contextMap.put(CURRENT_COMPONENT, previouslyPushed);
            } else {
                c = (UIComponent) contextMap.remove(CURRENT_COMPONENT);
            }

            if (c != null && UIComponent.isCompositeComponent(c)) {
                if (null != previouslyPushedCompositeComponent) {
                    contextMap.put(CURRENT_COMPOSITE_COMPONENT,
                                   previouslyPushedCompositeComponent);
                } else {
                    contextMap.remove(CURRENT_COMPOSITE_COMPONENT);
                }
            }
        }

    }


    /**
     * <p class="changed_added_2_0">Return <code>true</code> if
     * <code>component</code> is a composite component, otherwise
     * <code>false</code>.</p>
     *
     * @param component the {@link UIComponent} to test
     *
     * @throws NullPointerException if <code>component</code> is <code>null</code>
     * @since 2.0
     */
    public static boolean isCompositeComponent(UIComponent component) {

        if (component == null) {
            throw new NullPointerException();
        }
        return (component.getAttributes().containsKey(Resource.COMPONENT_RESOURCE_KEY));

    }


    /**
     * <p>
     * Finds the nearest composite component parent of the specified component.
     * </p>
     *
     * @param component the component from which to start the search from
     *
     * @return if <code>component</code> is <code>null</code>, return
     *  <code>null</code>, otherwise search the component's parent hierachy
     *  for the nearest parent composite component.  If no parent composite
     *  component is found, return <code>null</code>
     *
     * @since 2.0
     */
    public static UIComponent getCompositeComponentParent(UIComponent component) {

        if (component == null) {
            return null;
        } else {
            if (component.compositeParent != null) {
                return component.compositeParent;
            }
            UIComponent parent = component.getParent();
            while (parent != null) {
                if (UIComponent.isCompositeComponent(parent)) {
                    if (component.isInView()) {
                        component.compositeParent = parent;
                    }
                    return parent;
                }
                parent = parent.getParent();
            }
            return null;
        }
        
    }


    /**
     * <p class="changed_added_2_0">Return the <code>UIComponent</code>
     * instance that is currently processing.  This is equivalent to
     * evaluating the EL expression "<code>#{component}</code>" and
     * doing a <code>getValue</code> operation on the resultant
     * <code>ValueExpression</code>.</p>
     *
     * <p class="changed_added_2_0">This method must return
     * <code>null</code> if there is no currently processing
     * <code>UIComponent</code></p>
     *
     * @param context {@link FacesContext} for the request we are processing
     *
     * @throws NullPointerException if <code>context</code>
     *  is <code>null</code>
     *
     * @since 2.0
     */
    public static UIComponent getCurrentComponent(FacesContext context) {
        if (context == null) {
            throw new NullPointerException();
        }
        Map<Object, Object> contextMap = context.getAttributes();
        return (UIComponent) contextMap.get(CURRENT_COMPONENT);

    }


    /**
     * <p class="changed_added_2_0">Return the closest ancestor
     * component, relative to the component returned from {@link
     * #getCurrentComponent}, that is a composite component, or
     * <code>null</code> if no such component exists.</p>
     *
     * @param context {@link FacesContext} for the request we are processing
     * 
     * @throws NullPointerException if <code>context</code>
     *  is <code>null</code>
     *
     * @since 2.0
     */
    public static UIComponent getCurrentCompositeComponent(FacesContext context) {
        if (context == null) {
            throw new NullPointerException();
        }

        Map<Object, Object> contextMap = context.getAttributes();
        return (UIComponent) contextMap.get(CURRENT_COMPOSITE_COMPONENT);

    }
    
    // -------------------------------------------------- Event Listener Methods


    /**
     * <p>Add the specified {@link FacesListener} to the set of listeners
     * registered to receive event notifications from this {@link UIComponent}.
     * It is expected that {@link UIComponent} classes acting as event sources
     * will have corresponding typesafe APIs for registering listeners of the
     * required type, and the implementation of those registration methods
     * will delegate to this method.  For example:</p>
     * <pre>
     * public class FooEvent extends FacesEvent { ... }
     *
     * public interface FooListener extends FacesListener {
     *   public void processFoo(FooEvent event);
     * }
     *
     * public class FooComponent extends UIComponentBase {
     *   ...
     *   public void addFooListener(FooListener listener) {
     *     addFacesListener(listener);
     *   }
     *   public void removeFooListener(FooListener listener) {
     *     removeFacesListener(listener);
     *   }
     *   ...
     * }
     * </pre>
     *
     * @param listener The {@link FacesListener} to be registered
     *
     * @throws NullPointerException if <code>listener</code>
     *  is <code>null</code>
     */
    protected abstract void addFacesListener(FacesListener listener);


    /**
     * <p>Return an array of registered {@link FacesListener}s that are
     * instances of the specified class.  If there are no such registered
     * listeners, a zero-length array is returned.  The returned
     * array can be safely be cast to an array strongly typed to
     * an element type of <code>clazz</code>.</p>
     *
     * @param clazz Class that must be implemented by a {@link FacesListener}
     *  for it to be returned
     *
     * @throws IllegalArgumentException if <code>class</code> is not,
     *  and does not implement, {@link FacesListener}
     * @throws NullPointerException if <code>clazz</code>
     *  is <code>null</code>
     */
    protected abstract FacesListener[] getFacesListeners(Class clazz);


    /**
     * <p>Remove the specified {@link FacesListener} from the set of listeners
     * registered to receive event notifications from this {@link UIComponent}.
     *
     * @param listener The {@link FacesListener} to be deregistered
     *
     * @throws NullPointerException if <code>listener</code>
     *  is <code>null</code>
     */
    protected abstract void removeFacesListener(FacesListener listener);


    /**
     * <p>Queue an event for broadcast at the end of the current request
     * processing lifecycle phase.  The default implementation in
     * {@link UIComponentBase} must delegate this call to the
     * <code>queueEvent()</code> method of the parent {@link UIComponent}.</p>
     *
     * @param event {@link FacesEvent} to be queued
     *
     * @throws IllegalStateException if this component is not a
     *  descendant of a {@link UIViewRoot}
     * @throws NullPointerException if <code>event</code>
     *  is <code>null</code>
     */
    public abstract void queueEvent(FacesEvent event);

    /**
     * <p class="changed_added_2_0">Install the listener instance
     * referenced by argument <code>componentListener</code> as a
     * listener for events of type <code>eventClass</code> originating
     * from this specific instance of <code>UIComponent</code>.  The
     * default implementation creates an inner {@link
     * SystemEventListener} instance that wraps argument
     * <code>componentListener</code> as the <code>listener</code>
     * argument.  This inner class must call through to the argument
     * <code>componentListener</code> in its implementation of {@link
     * SystemEventListener#processEvent} and its implementation of
     * {@link SystemEventListener#isListenerForSource} must return
     * true if the instance class of this <code>UIComponent</code> is
     * assignable from the argument to
     * <code>isListenerForSource</code>.</p>
     *
     * @param eventClass the <code>Class</code> of event for which
     * <code>listener</code> must be fired.
     * @param componentListener the implementation of {@link
     * ComponentSystemEventListener} whose {@link
     * ComponentSystemEventListener#processEvent} method must be called
     * when events of type <code>facesEventClass</code> are fired.
     *
     * @throws <code>NullPointerException</code> if any of the
     * arguments are <code>null</code>.
     *
     * @since 2.0                                             
     */
    public void subscribeToEvent(Class<? extends SystemEvent> eventClass,
                                 ComponentSystemEventListener componentListener) {

        if (eventClass == null) {
            throw new NullPointerException();
        }
        if (componentListener == null) {
            throw new NullPointerException();
        }

        if (initialStateMarked()) {
            initialState = false;
        }
        if (null == listenersByEventClass) {
            listenersByEventClass = new HashMap<Class<? extends SystemEvent>,
                                                List<SystemEventListener>>(3, 1.0f);
        }
        SystemEventListener facesLifecycleListener =
              new ComponentSystemEventListenerAdapter(componentListener, this);
        List<SystemEventListener> listenersForEventClass =
              listenersByEventClass.get(eventClass);
        if (listenersForEventClass == null) {
            listenersForEventClass = new ArrayList<SystemEventListener>(3);
            listenersByEventClass.put(eventClass, listenersForEventClass);
        }
        if (!listenersForEventClass.contains(facesLifecycleListener)) {
            listenersForEventClass.add(facesLifecycleListener);
        }

    }

    /**
     * <p class="changed_added_2_0">Remove the listener instance
     *     referenced by argument <code>componentListener</code> as a
     *     listener for events of type <code>eventClass</code>
     *     originating from this specific instance of
     *     <code>UIComponent</code>.  When doing the comparison to
     *     determine if an existing listener is equal to the argument
     *     <code>componentListener</code> (and thus must be removed),
     *     the <code>equals()</code> method on the <em>existing
     *     listener</em> must be invoked, passing the argument
     *     <code>componentListener</code>, rather than the other way
     *     around.</p>
     *
     * @param eventClass the <code>Class</code> of event for which
     * <code>listener</code> must be removed.
     * @param componentListener the implementation of {@link
     * ComponentSystemEventListener} whose {@link
     * ComponentSystemEventListener#processEvent} method must no longer be called
     * when events of type <code>eventClass</code> are fired.
     *
     * @throws <code>NullPointerException</code> if any of the
     * arguments are <code>null</code>.
     *
     * @since 2.0
     */
    public void unsubscribeFromEvent(Class<? extends SystemEvent> eventClass,
                                     ComponentSystemEventListener componentListener) {

        if (eventClass == null) {
            throw new NullPointerException();
        }
        if (componentListener == null) {
            throw new NullPointerException();
        }

        List<SystemEventListener> listeners =
              getListenersForEventClass(eventClass);
        if (listeners != null && !listeners.isEmpty()) {
            for (Iterator<SystemEventListener> i = listeners.iterator(); i.hasNext();) {
                SystemEventListener item = i.next();
                ComponentSystemEventListenerAdapter csla =
                      (ComponentSystemEventListenerAdapter) item;
                ComponentSystemEventListener l = csla.getWrapped();
                if (l.equals(componentListener)) {
                    i.remove();
                    break;
                }
            }
        }

    }

    Map<Class<? extends SystemEvent>, List<SystemEventListener>> listenersByEventClass;

    /**
     * <p class="changed_added_2_0">Return the
     * <code>SystemEventListener</code> instances registered on this
     * <code>UIComponent</code> instance that are interested in events
     * of type <code>eventClass</code>.</p>
     *
     * @param eventClass the <code>Class</code> of event for which the
     * listeners must be returned.

     * @throws NullPointerException if argument <code>eventClass</code> is <code>null</code>.
     *
     * @since 2.0
     */
    public List<SystemEventListener> getListenersForEventClass(Class<? extends SystemEvent> eventClass) {

        if (eventClass == null) {
            throw new NullPointerException();
        }
        List<SystemEventListener> result = null;
        if (listenersByEventClass != null) {
            result = listenersByEventClass.get(eventClass);
        }
        return result;

    }


    /**
     * <p class="changed_added_2_0">Starting with "this", return the closest 
     * component in the ancestry that is a <code>NamingContainer</code>
     * or <code>null</code> if none can be found.</p>
     *
     * @since 2.0
     */
    public UIComponent getNamingContainer() {
        UIComponent namingContainer = this;
        while (namingContainer != null) {
            if (namingContainer instanceof NamingContainer) {
                return namingContainer;
            }
            namingContainer = namingContainer.getParent();
        }
        return null;
    }

    // ------------------------------------------------ Lifecycle Phase Handlers


    /**
     * <p><span class="changed_modified_2_0">Perform</span> the
     * component tree processing required by the <em>Restore View</em>
     * phase of the request processing lifecycle for all facets of this
     * component, all children of this component, and this component
     * itself, as follows.</p> <ul> <li
     * class="changed_modified_2_0">Call the <code>restoreState()</code>
     * method of this component.</li> 
     *
     * <li class="changed_added_2_0">Call
     * {@link UIComponent#pushComponentToEL}.  </li>

     * <li>Call the <code>processRestoreState()</code> method of all
     * facets and children of this {@link UIComponent} in the order
     * determined by a call to <code>getFacetsAndChildren()</code>.
     * <span class="changed_added_2_0">After returning from the
     * <code>processRestoreState()</code> method on a child or facet,
     * call {@link UIComponent#popComponentFromEL}</span></li>

     * </ul>
     *
     * <p>This method may not be called if the state saving method is
     * set to server.</p>
     *
     * @param context {@link FacesContext} for the request we are processing
     *
     * @throws NullPointerException if <code>context</code>
     *  is <code>null</code>
     */
    public abstract void processRestoreState(FacesContext context,
                                             Object state);


    /**
     * <p><span class="changed_modified_2_0">Perform</span> the
     * component tree processing required by the <em>Apply Request
     * Values</em> phase of the request processing lifecycle for all
     * facets of this component, all children of this component, and
     * this component itself, as follows.</p>

     * <ul>
     * <li>If the <code>rendered</code> property of this {@link UIComponent}
     *     is <code>false</code>, skip further processing.</li>
     * <li class="changed_added_2_0">Call {@link #pushComponentToEL}.</li>
     * <li>Call the <code>processDecodes()</code> method of all facets
     *     and children of this {@link UIComponent}, in the order determined
     *     by a call to <code>getFacetsAndChildren()</code>.</li>

     * <li>Call the <code>decode()</code> method of this component.</li>

     * <li>Call {@link #popComponentFromEL} from inside of a
     * <code>finally block, just before returning.</code></li>



     * <li>If a <code>RuntimeException</code> is thrown during
     *     decode processing, call {@link FacesContext#renderResponse}
     *     and re-throw the exception.</li>
     * </ul>
     *
     * @param context {@link FacesContext} for the request we are processing
     *
     * @throws NullPointerException if <code>context</code>
     *  is <code>null</code>
     */
    public abstract void processDecodes(FacesContext context);

    /**
     * <p class="changed_added_2_0">The default implementation performs
     * the following action.  If the argument <code>event</code> is an
     * instance of {@link PostRestoreStateEvent}, call
     * <code>this.</code>{@link #getValueExpression} passing the literal
     * string &#8220;binding&#8221;, without the quotes, as the
     * argument.  If the result is non-<code>null</code>, set the value
     * of the <code>ValueExpression</code> to be <code>this</code>.</p>
     */ 

    public void processEvent(ComponentSystemEvent event) throws AbortProcessingException {
        if (event instanceof PostRestoreStateEvent) {
	    assert(this == event.getComponent());
            // if this component has a component value reference expression,
            // make sure to populate the ValueExpression for it.
            ValueExpression valueExpression;
            if (null != (valueExpression = this.getValueExpression("binding"))) {
                valueExpression.setValue(FacesContext.getCurrentInstance().getELContext(), 
                        this);
            }

        }
    }
    
    


    /**
     * <p><span class="changed_modified_2_0">Perform</span> the
     * component tree processing required by the <em>Process
     * Validations</em> phase of the request processing lifecycle for
     * all facets of this component, all children of this component, and
     * this component itself, as follows.</p>

     * <ul>
     * <li>If the <code>rendered</code> property of this {@link UIComponent}
     *     is <code>false</code>, skip further processing.</li>
     * <li class="changed_added_2_0">Call {@link #pushComponentToEL}.</li>
     * <li>Call the <code>processValidators()</code> method of all facets
     *     and children of this {@link UIComponent}, in the order determined
     *     by a call to <code>getFacetsAndChildren()</code>.</li>
     * </ul>
     *
     * @param context {@link FacesContext} for the request we are processing
     *
     * @throws NullPointerException if <code>context</code>
     *  is <code>null</code>
     */
    public abstract void processValidators(FacesContext context);


    /**
     * <p><span class="changed_modified_2_0">Perform</span> the
     * component tree processing required by the <em>Update Model
     * Values</em> phase of the request processing lifecycle for all
     * facets of this component, all children of this component, and
     * this component itself, as follows.</p> 

     * <ul> 

     * <li>If the <code>rendered</code> property of this {@link
     * UIComponent} is <code>false</code>, skip further processing.</li>

     * <li class="changed_added_2_0">Call {@link
     * #pushComponentToEL}.</li>

     * <li>Call the <code>processUpdates()</code> method of all facets
     * and children of this {@link UIComponent}, in the order determined
     * by a call to <code>getFacetsAndChildren()</code>.  <span
     * class="changed_added_2_0">After returning from the
     * <code>processUpdates()</code> method on a child or facet, call
     * {@link UIComponent#popComponentFromEL}</span></li>
 
    * </ul>
     *
     * @param context {@link FacesContext} for the request we are processing
     *
     * @throws NullPointerException if <code>context</code>
     *  is <code>null</code>
     */
    public abstract void processUpdates(FacesContext context);


    /**
     * <p><span class="changed_modified_2_0">Perform</span> the
     * component tree processing required by the state saving portion of
     * the <em>Render Response</em> phase of the request processing
     * lifecycle for all facets of this component, all children of this
     * component, and this component itself, as follows.</p>

     * <ul>
     *
     * <li>consult the <code>transient</code> property of this
     * component.  If true, just return <code>null</code>.</li>

     * <li class="changed_added_2_0">Call {@link
     * #pushComponentToEL}.</li>

     * <li>Call the <code>processSaveState()</code> method of all facets
     * and children of this {@link UIComponent} in the order determined
     * by a call to <code>getFacetsAndChildren()</code>, skipping
     * children and facets that are transient.  Ensure that {@link
     * #popComponentFromEL} is called correctly after each child or
     * facet.</li>
     *
     * <li>Call the <code>saveState()</code> method of this component.</li>
     *
     * <li>Encapsulate the child state and your state into a
     * Serializable Object and return it.</li> 
     *
     * </ul>
     *
     * <p>This method may not be called if the state saving method is
     * set to server.</p>
     *
     * @param context {@link FacesContext} for the request we are processing
     *
     * @throws NullPointerException if <code>context</code>
     *  is <code>null</code>
     */
    public abstract Object processSaveState(FacesContext context);


    // ----------------------------------------------------- Convenience Methods


    /**
     * <p>Convenience method to return the {@link FacesContext} instance
     * for the current request.</p>
     */
    protected abstract FacesContext getFacesContext();


    /**
     * <p>Convenience method to return the {@link Renderer} instance
     * associated with this component, if any; otherwise, return
     * <code>null</code>.</p>
     *
     * @param context {@link FacesContext} for the current request
     */
    protected abstract Renderer getRenderer(FacesContext context);


    // --------------------------------------------------------- Package Private


    static final class ComponentSystemEventListenerAdapter
       implements SystemEventListener, StateHolder, FacesWrapper<ComponentSystemEventListener> {

        ComponentSystemEventListener wrapped;
        Class<?> instanceClass;


        // -------------------------------------------------------- Constructors


        ComponentSystemEventListenerAdapter() {

            // necessary for state saving

        }


        ComponentSystemEventListenerAdapter(ComponentSystemEventListener wrapped,
                                            UIComponent component) {

            this.wrapped = wrapped;
            this.instanceClass = component.getClass();

        }


        // ------------------------------------ Methods from SystemEventListener


        public void processEvent(SystemEvent event) throws AbortProcessingException {

            wrapped.processEvent((ComponentSystemEvent) event);

        }


        public boolean isListenerForSource(Object component) {

            if (wrapped instanceof SystemEventListener) {
                return ((SystemEventListener) wrapped).isListenerForSource(component);
            } else {
                return instanceClass.isAssignableFrom(component.getClass());
            }

        }


        // -------------------------------------------- Methods from StateHolder

        public Object saveState(FacesContext context) {

            return new Object[] {
                  ((wrapped instanceof UIComponent) ? null : new StateHolderSaver(context, wrapped)),
                  instanceClass
            };

        }


        public void restoreState(FacesContext context, Object state) {

            if (state == null) {
                return;
            }
            Object[] s = (Object[]) state;
            Object listener = s[0];
            wrapped = (ComponentSystemEventListener) ((listener == null)
                                                      ? UIComponent .getCurrentComponent(context)
                                                      : ((StateHolderSaver) listener).restore(context));
            instanceClass = (Class<?>) s[1];
            
        }


        public boolean isTransient() {

            return false;

        }


        public void setTransient(boolean newTransientValue) {

            // no-op

        }


        // ------------------------------------------- Methods from FacesWrapper


        public ComponentSystemEventListener getWrapped() {

            return wrapped;

        }


        // ------------------------------------------------------ Public Methods


        @Override
        public int hashCode() {

            return (wrapped.hashCode() ^ instanceClass.hashCode());

        }

        @Override
        public boolean equals(Object obj) {

            if (!(obj instanceof ComponentSystemEventListenerAdapter)) {
                return false;
            }
            ComponentSystemEventListenerAdapter in =
                  (ComponentSystemEventListenerAdapter) obj;
            return (wrapped.getClass().equals(in.wrapped.getClass())
                    && instanceClass.equals(in.instanceClass));
            
        }
    } // END ComponentSystemEventListenerAdapter

}
