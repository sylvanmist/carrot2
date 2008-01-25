package org.carrot2.core.attribute;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;

import org.carrot2.core.constraint.Constraint;
import org.carrot2.core.constraint.ConstraintViolationException;

public class AttributeBinder
{
    /**
     * Contract:
     * <ul>
     * <li>Attributes are optional</li>
     * <li>Attributes don't have to have default values</li>
     * <li>Map can contain null values, these will be transferred to the fields</li>
     * <li>If the map doesn't have a mapping for some key, the corresponding field will
     * not be changed</li>
     * <li>Class coercion is also performed for all binding times</li>
     * </ul>
     */
    public static <T> void bind(T instance, Map<String, Object> values,
        Class<? extends Annotation> bindingTimeAnnotation,
        Class<? extends Annotation> bindingDirectionAnnotation)
        throws InstantiationException
    {
        bind(instance, values, bindingTimeAnnotation, bindingDirectionAnnotation,
            new HashSet<Object>());
    }

    static <T> void bind(T instance, Map<String, Object> values,
        Class<? extends Annotation> bindingTimeAnnotation,
        Class<? extends Annotation> bindingDirectionAnnotation, Set<Object> boundInstances)
        throws InstantiationException
    {
        // We can only bind values on classes that are @Bindable
        if (instance.getClass().getAnnotation(Bindable.class) == null)
        {
            throw new IllegalArgumentException("Class is not bindable: "
                + instance.getClass().getName());
        }
        
        // To detect circulare references
        boundInstances.add(instance);

        // Get all fields (including those from bindable super classes)
        final Collection<Field> fieldSet = BindableUtils
            .getFieldsFromBindableHierarchy(instance.getClass());

        for (Field field : fieldSet)
        {
            final String key = BindableUtils.getKey(field);
            Object value = null;

            // We skip fields that do not have the required binding time
            if (!(field.getAnnotation(bindingTimeAnnotation) == null))
            {
                // Choose the right direction
                if (Input.class.equals(bindingDirectionAnnotation)
                    && field.getAnnotation(Input.class) != null)
                {
                    // Transfer values from the map to the fields.
                    // If the input map doesn't contain an entry for this key, do nothing
                    // Otherwise, perform binding as usual. This will allow to set null
                    // values
                    if (!values.containsKey(key))
                    {
                        continue;
                    }

                    // Note that the value can still be null here
                    value = values.get(key);

                    // Try to coerce from class to its instance first
                    if (value instanceof Class)
                    {
                        Class<?> clazz = ((Class<?>) value);
                        try
                        {
                            value = createInstance(clazz, values);
                        }
                        catch (InstantiationException e)
                        {
                            throw new InstantiationException(
                                "Could not create instance of class: " + clazz.getName()
                                    + " for parameter " + key);
                        }
                    }

                    if (value != null)
                    {
                        // Check constraints
                        Constraint constraint = BindableUtils.getConstraint(field);
                        if (constraint != null)
                        {
                            if (!constraint.isMet(value))
                            {
                                throw new ConstraintViolationException(key, constraint,
                                    value);
                            }
                        }
                    }

                    // Finally, set the field value
                    try
                    {
                        field.setAccessible(true);
                        field.set(instance, value);
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException("Could not assign field "
                            + instance.getClass().getName() + "#" + field.getName()
                            + " with value " + value, e);
                    }
                }
                else if (Output.class.equals(bindingDirectionAnnotation)
                    && field.getAnnotation(Output.class) != null)
                {
                    // Transfer values from fields to the map here
                    try
                    {
                        field.setAccessible(true);
                        value = field.get(instance);
                        values.put(key, value);
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException("Could not get field value "
                            + instance.getClass().getName() + "#" + field.getName());
                    }
                }
            }
            else
            {
                try
                {
                    field.setAccessible(true);
                    value = field.get(instance);
                }
                catch (Exception e)
                {
                    throw new RuntimeException("Could not get field value "
                        + instance.getClass().getName() + "#" + field.getName());
                }
            }

            // If value is not null and its class is @Bindable, we must descend into it
            if (value != null && value.getClass().getAnnotation(Bindable.class) != null)
            {
                // Check for circular references
                if (boundInstances.contains(value))
                {
                    throw new UnsupportedOperationException(
                        "Binding circular references not is supported");
                }
                
                // Recursively descend into other types.
                bind(value, values, bindingTimeAnnotation, bindingDirectionAnnotation);
            }
        }
    }

    /**
     * Create an instance of a given class and initialize it with default values of
     * instance-time binding parameters ({@link BindingPolicy#INSTANTIATION}).
     */
    public static <T> T createInstance(Class<T> clazz, Map<String, Object> values)
        throws InstantiationException
    {
        final T instance;
        try
        {
            instance = clazz.newInstance();
        }
        catch (IllegalAccessException e)
        {
            throw new InstantiationException("Could not create instance: " + e);
        }

        bind(instance, values, Init.class, Input.class);

        return instance;
    }
}
