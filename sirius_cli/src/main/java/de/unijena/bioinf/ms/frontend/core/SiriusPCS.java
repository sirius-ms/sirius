/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.core;

import de.unijena.bioinf.ms.annotations.DataAnnotation;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.SwingPropertyChangeSupport;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * <p>
 * A convenience class from which to extend all non-visual AbstractBeans. It
 * manages the PropertyChange notification system, making it relatively trivial
 * to add support for property change events in getters/setters.
 * </p>
 * <p>
 * <p>
 * A non-visual java bean is a Java class that conforms to the AbstractBean
 * patterns to allow visual manipulation of the bean's properties and event
 * handlers at design-time.
 * </p>
 * <p>
 * <p>
 * Here is a simple example bean that contains one property, foo, and the proper
 * pattern for implementing property change notification:
 * <p>
 * <pre><code>
 * public class ABean extends AbstractBean {
 *     private String foo;
 *
 *     public void setFoo(String newFoo) {
 *         String old = getFoo();
 *         this.foo = newFoo;
 *         firePropertyChange(&quot;foo&quot;, old, getFoo());
 *     }
 *
 *     public String getFoo() {
 *         return foo;
 *     }
 * }
 * </code></pre>
 * <p>
 * </p>
 * <p>
 * <p>
 * You will notice that "getFoo()" is used in the setFoo method rather than
 * accessing "foo" directly for the gets. This is done intentionally so that if
 * a subclass overrides getFoo() to return, for instance, a constant value the
 * property change notification system will continue to work properly.
 * </p>
 * <p>
 * <p>
 * The firePropertyChange method takes into account the old value and the new
 * value. Only if the two differ will it fire a property change event. So you
 * can be assured from the above code fragment that a property change event will
 * only occur if old is indeed different from getFoo()
 * </p>
 * <p>
 * <p>
 * <code>AbstractBean</code> also supports vetoable
 * {@link PropertyChangeEvent} events. These events are similar to
 * <code>PropertyChange</code> events, except a special exception can be used
 * to veto changing the property. For example, perhaps the property is changing
 * from "fred" to "red", but a listener deems that "red" is unexceptable. In
 * this case, the listener can fire a veto exception and the property must
 * remain "fred". For example:
 * <p>
 * <pre><code>
 *  public class ABean extends AbstractBean {
 *    private String foo;
 *
 *    public void setFoo(String newFoo) throws PropertyVetoException {
 *      String old = getFoo();
 *      this.foo = newFoo;
 *      fireVetoableChange(&quot;foo&quot;, old, getFoo());
 *    }
 *    public String getFoo() {
 *      return foo;
 *    }
 *  }
 *
 *  public class Tester {
 *    public static void main(String... args) {
 *      try {
 *        ABean a = new ABean();
 *        a.setFoo(&quot;fred&quot;);
 *        a.addVetoableChangeListener(new VetoableChangeListener() {
 *          public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
 *            if (&quot;red&quot;.equals(evt.getNewValue()) {
 *              throw new PropertyVetoException(&quot;Cannot be red!&quot;, evt);
 *            }
 *          }
 *        }
 *        a.setFoo(&quot;red&quot;);
 *      } catch (Exception e) {
 *        e.printStackTrace(); // this will be executed
 *      }
 *    }
 *  }
 * </code></pre>
 * <p>
 * </p>
 * <p>
 * {@code AbstractBean} is not {@link java.io.Serializable}. Special care must
 * be taken when creating {@code Serializable} subclasses, as the
 * {@code Serializable} listeners will not be saved.  Subclasses will need to
 * manually save the serializable listeners.   If
 * possible, it is recommended that {@code Serializable} beans should extend
 * {@code AbstractSerializableBean}.  If it is not possible, the
 * {@code AbstractSerializableBean} bean implementation provides details on
 * how to correctly serialize an {@code AbstractBean} subclass.
 * </p>
 *
 * @author rbair
 */
@SuppressWarnings("nls")
public interface SiriusPCS {


    /**
     * Add a PropertyChangeListener to the listener list.
     * The listener is registered for all properties.
     * The same listener object may be added more than once, and will be called
     * as many times as it is added.
     * If <code>listener</code> is null, no exception is thrown and no action
     * is taken.
     *
     * @param listener The PropertyChangeListener to be added
     */
    default void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs().pcs.addPropertyChangeListener(listener);
    }

    /**
     * Remove a PropertyChangeListener from the listener list.
     * This removes a PropertyChangeListener that was registered
     * for all properties.
     * If <code>listener</code> was added more than once to the same event
     * source, it will be notified one less time after being removed.
     * If <code>listener</code> is null, or was never added, no exception is
     * thrown and no action is taken.
     *
     * @param listener The PropertyChangeListener to be removed
     */
    default void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs().pcs.removePropertyChangeListener(listener);
    }

    /**
     * Returns an array of all the listeners that were added to the
     * PropertyChangeSupport object with addPropertyChangeListener().
     * <p>
     * If some listeners have been added with a named property, then
     * the returned array will be a mixture of PropertyChangeListeners
     * and <code>PropertyChangeListenerProxy</code>s. If the calling
     * method is interested in distinguishing the listeners then it must
     * test each element to see if it's a
     * <code>PropertyChangeListenerProxy</code>, perform the cast, and examine
     * the parameter.
     * <p>
     * <pre>
     * PropertyChangeListener[] listeners = bean.getPropertyChangeListeners();
     * for (int i = 0; i &lt; listeners.length; i++) {
     *     if (listeners[i] instanceof PropertyChangeListenerProxy) {
     *     PropertyChangeListenerProxy proxy =
     *                    (PropertyChangeListenerProxy)listeners[i];
     *     if (proxy.getPropertyName().equals("foo")) {
     *       // proxy is a PropertyChangeListener which was associated
     *       // with the property named "foo"
     *     }
     *   }
     * }
     * </pre>
     *
     * @return all of the <code>PropertyChangeListeners</code> added or an
     * empty array if no listeners have been added
     * @see java.beans.PropertyChangeListenerProxy
     */
    default PropertyChangeListener[] getPropertyChangeListeners() {
        return pcs().pcs.getPropertyChangeListeners();
    }

    /**
     * Add a PropertyChangeListener for a specific property.  The listener
     * will be invoked only when a call on firePropertyChange names that
     * specific property.
     * The same listener object may be added more than once.  For each
     * property,  the listener will be invoked the number of times it was added
     * for that property.
     * If <code>propertyName</code> or <code>listener</code> is null, no
     * exception is thrown and no action is taken.
     *
     * @param propertyName The name of the property to listen on.
     * @param listener     The PropertyChangeListener to be added
     */
    default void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs().pcs.addPropertyChangeListener(propertyName, listener);
    }

    default void addPropertyChangeListener(Class<? extends DataAnnotation> property, PropertyChangeListener listener) {
        addPropertyChangeListener(DataAnnotation.getIdentifier(property), listener);
    }

    /**
     * Remove a PropertyChangeListener for a specific property.
     * If <code>listener</code> was added more than once to the same event
     * source for the specified property, it will be notified one less time
     * after being removed.
     * If <code>propertyName</code> is null,  no exception is thrown and no
     * action is taken.
     * If <code>listener</code> is null, or was never added for the specified
     * property, no exception is thrown and no action is taken.
     *
     * @param propertyName The name of the property that was listened on.
     * @param listener     The PropertyChangeListener to be removed
     */
    default void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs().pcs.removePropertyChangeListener(propertyName, listener);
    }

    default void removePropertyChangeListener(Class<? extends DataAnnotation> property, PropertyChangeListener listener) {
        removePropertyChangeListener(DataAnnotation.getIdentifier(property), listener);
    }

    /**
     * Returns an array of all the listeners which have been associated
     * with the named property.
     *
     * @param propertyName The name of the property being listened to
     * @return all of the <code>PropertyChangeListeners</code> associated with
     * the named property.  If no such listeners have been added,
     * or if <code>propertyName</code> is null, an empty array is
     * returned.
     */
    default PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return pcs().pcs.getPropertyChangeListeners(propertyName);
    }


    HiddenChangeSupport pcs();

    /**
     * This allows us to hide the PropertyChangeSupport from the outside
     * but inject it from the class that implements the interface.
     * So we can implement all annotation functionality within this interface
     * instead of each class separately.
     */
    abstract class HiddenChangeSupport {
        /**
         * Helper class that manages all the property change notification machinery.
         * PropertyChangeSupport cannot be extended directly because it requires
         * a bean in the constructor, and the "this" argument is not valid until
         * after super construction. Hence, delegation instead of extension
         */
        final transient PropertyChangeSupport pcs;

        protected HiddenChangeSupport(@NotNull final Object sourceBean, final boolean notifyOnEDT) {
            pcs = new SwingPropertyChangeSupport(sourceBean, notifyOnEDT);
        }
    }

    final class MutableHiddenChangeSupport extends HiddenChangeSupport {

        public MutableHiddenChangeSupport(@NotNull Object sourceBean, boolean notifyOnEDT) {
            super(sourceBean, notifyOnEDT);
        }

        /**
         * Report a bound property update to any registered listeners.
         * No event is fired if old and new are equal and non-null.
         * <p>
         * <p>
         * This is merely a convenience wrapper around the more general
         * firePropertyChange method that takes {@code
         * PropertyChangeEvent} value.
         *
         * @param propertyName The programmatic name of the property
         *                     that was changed.
         * @param oldValue     The old value of the property.
         * @param newValue     The new value of the property.
         */
        public final void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
            pcs.firePropertyChange(propertyName, oldValue, newValue);
        }

        /**
         * Fire an existing PropertyChangeEvent to any registered listeners.
         * No event is fired if the given event's old and new values are
         * equal and non-null.
         *
         * @param evt The PropertyChangeEvent object.
         */
        public final void firePropertyChange(PropertyChangeEvent evt) {
            pcs.firePropertyChange(evt);
        }


        /**
         * Report a bound indexed property update to any registered
         * listeners.
         * <p>
         * No event is fired if old and new values are equal
         * and non-null.
         * <p>
         * <p>
         * This is merely a convenience wrapper around the more general
         * firePropertyChange method that takes {@code PropertyChangeEvent} value.
         *
         * @param propertyName The programmatic name of the property that
         *                     was changed.
         * @param index        index of the property element that was changed.
         * @param oldValue     The old value of the property.
         * @param newValue     The new value of the property.
         */
        public final void fireIndexedPropertyChange(String propertyName, int index,
                                                       Object oldValue, Object newValue) {
            pcs.fireIndexedPropertyChange(propertyName, index, oldValue, newValue);
        }

        /**
         * Check if there are any listeners for a specific property, including
         * those registered on all properties.  If <code>propertyName</code>
         * is null, only check for listeners registered on all properties.
         *
         * @param propertyName the property name.
         * @return true if there are one or more listeners for the given property
         */
        public final boolean hasPropertyChangeListeners(String propertyName) {
            return pcs.hasListeners(propertyName);
        }
    }

}
