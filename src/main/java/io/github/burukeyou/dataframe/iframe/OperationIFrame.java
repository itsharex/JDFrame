package io.github.burukeyou.dataframe.iframe;

import java.util.Collection;

/**
 *
 * @author      caizhihao
 * @param <T>
 */
public interface OperationIFrame<T> {

    /**
     * union other frame
     * @param other         other frame
     */
    IFrame<T> unionAll(IFrame<T> other);

    /**
     * union other frame, will delete duplicates
     * @param other         other frame
     */
    IFrame<T> union(IFrame<T> other);

    /**
     *  Retains only the elements in this list that are contained in the specified collection
     * @return           other frame
     */
    IFrame<T> retainAll(IFrame<T> other);

    /**
     * Retains only the elements in this list that are contained in the specified collection
     * @return           other collection
     */
    IFrame<T> retainAll(Collection<T> other);

    /**
     * intersection other frame
     *          get identical elements from two sets
     * @param other     other frame
     */
    IFrame<T> intersection(IFrame<T> other);

    /**
     * intersection other frame
     *          get identical elements from two sets
     * @param other     other collection
     */
    IFrame<T> intersection(Collection<T> other);

    /**
     * different other frame
     *      Elements that are not within the other frame
     * @return           other frame
     */
    IFrame<T> different(IFrame<T> other);
}
