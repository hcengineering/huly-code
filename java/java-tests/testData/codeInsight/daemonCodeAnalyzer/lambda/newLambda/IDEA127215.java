
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;


abstract class FooBar<M> {

  abstract void collect(List<? extends M> collector);

  void foo(FooBar<?> objectStream) {
    objectStream. collect(toList( ));

  }

  static <T> List<T> toList() {
    return null;
  }

}
class Test {
  <T> List<List<Object>> foo(List<T> objects, Function<T, ?>... functions) {
    return objects.stream()
      .map(object -> Arrays.stream(functions)
        .map(fn -> fn.apply(object))
        .collect(toList()))
      .<error descr="Incompatible types. Found: 'java.util.List<java.util.List<capture<?>>>', required: 'java.util.List<java.util.List<java.lang.Object>>'">collect</error>(toList());
  }
}