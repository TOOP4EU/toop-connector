package eu.toop.connector.me.notifications;

/**
 * A bag object that carries another object. Equivalent to a one element array. Used for carrying objects between
 * threads. Can be used for synchronized waiting
 *
 * @author yerlibilgin
 */
public class LockableObjectCarrier<T> {

  private T actualObject;

  public T getActualObject() {
    return actualObject;
  }

  public void setActualObject(T actualObject) {
    this.actualObject = actualObject;
  }
}
