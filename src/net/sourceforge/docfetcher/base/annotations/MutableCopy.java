package net.sourceforge.docfetcher.base.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated Iterable is a mutable copy of another Iterable.
 * This implies that the copy can be safely iterated over while the original
 * Iterable is being modified (either by the same or by a different thread).
 * 
 * @author Tran Nam Quang
 */
@Target({
	ElementType.FIELD,
	ElementType.METHOD
})
@Retention(RetentionPolicy.SOURCE)
public @interface MutableCopy {

}