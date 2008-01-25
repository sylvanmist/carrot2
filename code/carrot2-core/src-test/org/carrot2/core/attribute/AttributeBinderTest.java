/**
 * 
 */
package org.carrot2.core.attribute;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.carrot2.core.constraint.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class AttributeBinderTest
{
    private Map<String, Object> attributes;

    @Bindable
    @SuppressWarnings("unused")
    public static class SingleClass
    {
        @Init
        @Input
        @Attribute
        private int initInput = 5;

        @Init
        @Output
        @Attribute
        private int initOutput = 10;

        @Processing
        @Input
        @Attribute
        private int processingInput = 5;

        @Processing
        @Output
        @Attribute
        private int processingOutput = 10;
    }

    @Bindable
    @SuppressWarnings("unused")
    public static class SuperClass
    {
        @Processing
        @Input
        @Attribute
        private int processingInput = 5;

        @Processing
        @Output
        @Attribute
        private int processingOutput = 9;
    }

    @Bindable
    @SuppressWarnings("unused")
    public static class SubClass extends SuperClass
    {
        @Processing
        @Input
        @Attribute
        private int processingInput = 5;

        @Processing
        @Output
        @Attribute
        private int processingOutput = 5;
    }

    @Bindable
    public static class BindableReferenceContainer
    {
        private BindableReference bindableReference = new BindableReference();

        private NotBindable notBindableReference = new NotBindable();
    }

    @Bindable
    @SuppressWarnings("unused")
    public static class BindableReference
    {
        @Processing
        @Input
        @Attribute
        private int processingInput = 5;

        @Processing
        @Output
        @Attribute
        private int processingOutput = 5;
    }

    @Bindable
    @SuppressWarnings("unused")
    public static class CircularReferenceContainer
    {
        @Processing
        @Input
        @Output
        @Attribute
        private CircularReferenceContainer circular;
    }

    @Bindable
    @SuppressWarnings("unused")
    public static class SimpleConstraint
    {
        @Processing
        @Input
        @Attribute
        @IntRange(min = 0, max = 10)
        private int processingInput = 5;
    }

    @Bindable
    @SuppressWarnings("unused")
    public static class CompoundConstraint
    {
        @Processing
        @Input
        @Attribute
        @IntRange(min = 0, max = 10)
        @IntModulo(modulo = 3)
        private int processingInput = 3;
    }

    @Bindable
    public static class CoercedReferenceContainer
    {
        @Input
        @Processing
        @Attribute
        private CoercedInterface coerced = null;
    }

    public static interface CoercedInterface
    {
    }

    @Bindable
    @SuppressWarnings("unused")
    public static class CoercedInterfaceImpl implements CoercedInterface
    {
        @Init
        @Input
        @Attribute
        private int initInput = 5;
    }

    @Bindable(prefix = "Prefix")
    @SuppressWarnings("unused")
    public static class ClassWithPrefix
    {
        @Init
        @Input
        @Attribute(key = "init")
        private int initInput = 5;

        @Processing
        @Input
        @Attribute
        private int processingInput = 10;
    }

    @Bindable
    public static class NullReferenceContainer
    {
        @Processing
        @Input
        @Attribute
        private BindableReference processingInput = null;
    }
    
    @SuppressWarnings("unused")
    public static class NotBindable
    {
        @Processing
        @Input
        @Attribute
        private int processingInput = 5;
    }

    @Before
    public void initAttributes()
    {
        attributes = new HashMap<String, Object>();
    }

    @Test
    public void testSingleClassInput() throws InstantiationException
    {
        SingleClass instance;

        addAttribute(SingleClass.class, "initInput", 6);
        addAttribute(SingleClass.class, "processingInput", 6);

        instance = new SingleClass();
        AttributeBinder.bind(instance, attributes, Init.class, Input.class);
        checkFieldValues(instance, new Object []
        {
            "initInput", 6, "processingInput", 5, "initOutput", 10, "processingOutput",
            10
        });

        instance = new SingleClass();
        AttributeBinder.bind(instance, attributes, Processing.class, Input.class);
        checkFieldValues(instance, new Object []
        {
            "initInput", 5, "processingInput", 6, "initOutput", 10, "processingOutput",
            10
        });
    }

    @Test
    public void testSingleClassOutput() throws InstantiationException
    {
        SingleClass instance = new SingleClass();

        AttributeBinder.bind(instance, attributes, Init.class, Output.class);
        checkAttributeValues(instance.getClass(), new Object []
        {
            "initOutput", 10
        });

        attributes.clear();
        AttributeBinder.bind(instance, attributes, Processing.class, Output.class);
        checkFieldValues(instance, new Object []
        {
            "processingOutput", 10
        });
    }

    @Test
    public void testBindableHierarchyInput() throws InstantiationException
    {
        SubClass instance = new SubClass();

        addAttribute(SubClass.class, "processingInput", 6);
        addAttribute(SuperClass.class, "processingInput", 7);

        AttributeBinder.bind(instance, attributes, Processing.class, Input.class);
        checkFieldValues(instance, new Object []
        {
            "processingInput", 6
        });
        checkFieldValues(instance, SuperClass.class, new Object []
        {
            "processingInput", 7
        });
    }

    @Test
    public void testBindableHierarchyOutput() throws InstantiationException
    {
        SubClass instance = new SubClass();

        AttributeBinder.bind(instance, attributes, Processing.class, Output.class);
        checkAttributeValues(SubClass.class, new Object []
        {
            "processingOutput", 5
        });
        checkAttributeValues(SuperClass.class, new Object []
        {
            "processingOutput", 9
        });
    }

    @Test
    public void testReferenceInput() throws InstantiationException
    {
        BindableReferenceContainer instance = new BindableReferenceContainer();
        addAttribute(BindableReference.class, "processingInput", 6);
        addAttribute(NotBindable.class, "processingInput", 7);

        AttributeBinder.bind(instance, attributes, Processing.class, Input.class);

        checkFieldValues(instance.bindableReference, new Object []
        {
            "processingInput", 6
        });

        // Not bindable field must not change
        checkFieldValues(instance.notBindableReference, new Object []
        {
            "processingInput", 5
        });
    }

    @Test
    public void testReferenceOutput() throws InstantiationException
    {
        BindableReferenceContainer instance = new BindableReferenceContainer();

        AttributeBinder.bind(instance, attributes, Processing.class, Output.class);
        checkAttributeValues(BindableReference.class, new Object []
        {
            "processingOutput", 5
        });

        // Not bindable fields must not be collected
        assertFalse("Fields from not bindables not collected", attributes
            .containsKey(getKey(NotBindable.class, "processingInput")));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCircularReferences() throws InstantiationException
    {
        CircularReferenceContainer instance = new CircularReferenceContainer();
        instance.circular = instance;

        addAttribute(CircularReferenceContainer.class, "circular", instance);

        AttributeBinder.bind(instance, attributes, Processing.class, Input.class);
    }

    @Test
    public void testSimpleConstraints() throws InstantiationException
    {
        SimpleConstraint instance = new SimpleConstraint();

        addAttribute(SimpleConstraint.class, "processingInput", 2);
        AttributeBinder.bind(instance, attributes, Processing.class, Input.class);
        checkFieldValues(instance, new Object []
        {
            "processingInput", 2
        });

        addAttribute(SimpleConstraint.class, "processingInput", 12);
        try
        {
            AttributeBinder.bind(instance, attributes, Processing.class, Input.class);
            fail();
        }
        catch (ConstraintViolationException e)
        {
            assertEquals(12, e.getOffendingValue());
        }
        checkFieldValues(instance, new Object []
        {
            "processingInput", 2
        });
    }

    @Test
    public void testCompoundConstraints() throws InstantiationException
    {
        CompoundConstraint instance = new CompoundConstraint();

        addAttribute(CompoundConstraint.class, "processingInput", 9);
        AttributeBinder.bind(instance, attributes, Processing.class, Input.class);
        checkFieldValues(instance, new Object []
        {
            "processingInput", 9
        });

        addAttribute(CompoundConstraint.class, "processingInput", 8);
        try
        {
            AttributeBinder.bind(instance, attributes, Processing.class, Input.class);
            fail();
        }
        catch (ConstraintViolationException e)
        {
            assertEquals(8, e.getOffendingValue());
        }
        checkFieldValues(instance, new Object []
        {
            "processingInput", 9
        });

        addAttribute(CompoundConstraint.class, "processingInput", 12);
        try
        {
            AttributeBinder.bind(instance, attributes, Processing.class, Input.class);
            fail();
        }
        catch (ConstraintViolationException e)
        {
            assertEquals(12, e.getOffendingValue());
        }
        checkFieldValues(instance, new Object []
        {
            "processingInput", 9
        });
    }

    @Test
    public void testClassCoercion() throws InstantiationException
    {
        CoercedReferenceContainer instance = new CoercedReferenceContainer();

        addAttribute(CoercedReferenceContainer.class, "coerced",
            CoercedInterfaceImpl.class);
        addAttribute(CoercedInterfaceImpl.class, "initInput", 7);

        AttributeBinder.bind(instance, attributes, Processing.class, Input.class);
        assertNotNull(instance.coerced);
        assertEquals(instance.coerced.getClass(), CoercedInterfaceImpl.class);
        checkFieldValues(instance.coerced, new Object []
        {
            "initInput", 7
        });
    }

    @Test
    public void testPrefixing() throws InstantiationException
    {
        ClassWithPrefix instance = new ClassWithPrefix();

        attributes.put("init", 7);
        attributes.put("Prefix.procesingField", 6);

        AttributeBinder.bind(instance, attributes, Init.class, Input.class);
        AttributeBinder.bind(instance, attributes, Processing.class, Input.class);

        checkFieldValues(instance, new Object []
        {
            "initInput", 7, "processingInput", 6
        });
    }

    @Test
    public void testNullReference() throws InstantiationException
    {
        NullReferenceContainer instance = new NullReferenceContainer();
        
        addAttribute(BindableReference.class, "processingInput", 10);
        
        // Neither @Input nor @Output binding can fail
        AttributeBinder.bind(instance, attributes, Processing.class, Input.class);
        AttributeBinder.bind(instance, attributes, Processing.class, Output.class);
    }
    
    @Test
    public void testNullInputAttributes() throws InstantiationException
    {
        NullReferenceContainer instance = new NullReferenceContainer();
        instance.processingInput = new BindableReference();
        
        addAttribute(NullReferenceContainer.class, "processingInput", null);
        
        AttributeBinder.bind(instance, attributes, Processing.class, Input.class);
        
        assertNull(instance.processingInput);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNotBindable() throws InstantiationException
    {
        NotBindable instance = new NotBindable();
        AttributeBinder.bind(instance, attributes, Init.class, Input.class);
    }

    private void addAttribute(Class<?> clazz, String field, Object value)
    {
        attributes.put(getKey(clazz, field), value);
    }

    private String getKey(Class<?> clazz, String fieldName)
    {
        if (clazz.getAnnotation(Bindable.class) != null)
        {
            return BindableUtils.getKey(clazz, fieldName);
        }
        else
        {
            return clazz.getName() + "." + fieldName;
        }
    }

    private void checkFieldValues(Object instance, Object [] fieldNamesValues)
    {
        checkFieldValues(instance, instance.getClass(), fieldNamesValues);
    }

    private void checkFieldValues(Object instance, Class<?> clazz,
        Object [] fieldNamesValues)
    {
        assertTrue(fieldNamesValues.length % 2 == 0);
        for (int i = 0; i < fieldNamesValues.length / 2; i += 2)
        {
            final String fieldName = (String) fieldNamesValues[i];
            final Object expectedFieldValue = fieldNamesValues[i + 1];

            Object actualFieldValue = null;
            try
            {
                final Field declaredField = clazz.getDeclaredField(fieldName);
                declaredField.setAccessible(true);

                actualFieldValue = declaredField.get(instance);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }

            assertEquals("Value of " + clazz.getName() + "#" + fieldName,
                expectedFieldValue, actualFieldValue);
        }
    }

    private void checkAttributeValues(Class<?> clazz, Object [] keysValues)
    {
        assertTrue(keysValues.length % 2 == 0);

        for (int i = 0; i < keysValues.length / 2; i += 2)
        {
            final String key = clazz.getName() + "." + (String) keysValues[i];
            final Object expectedValue = keysValues[i + 1];
            final Object actualValue = attributes.get(key);

            assertEquals("Value of " + clazz.getName() + "#" + keysValues[i],
                expectedValue, actualValue);
        }
    }
}
