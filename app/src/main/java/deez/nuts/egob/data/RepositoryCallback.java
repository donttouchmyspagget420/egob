package deez.nuts.egob.data;

/** Result callback used by the repository. Always delivered on the main thread in production. */
public interface RepositoryCallback<T> {
    void onSuccess(T value);

    void onError(String message);
}
