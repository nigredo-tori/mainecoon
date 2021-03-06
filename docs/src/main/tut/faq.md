---
layout: page
title:  FAQ
section: faq
position: 2
---


## FAQs

### Does mainecoon support algebras with extra type parameters?

Yes. e.g.

```tut:silent
import mainecoon._
import cats.~>
import util.Try

@autoFunctorK @finalAlg
trait Foo[F[_], T] {
  def a(i: Int): F[T]
}

implicit val tryFoo: Foo[Try, String] = new Foo[Try, String] {
  def a(i: Int) = Try(i.toString)
}

implicit val fk: Try ~> Option = λ[Try ~> Option](_.toOption)

import Foo.autoDerive._
```
```tut:book

Foo[Option, String].a(3)
```

### Does mainecoon support algebras with abstract type member?

Yes but with some caveats.
The `FunctorK` instance it generates does not refine to the type member. E.g.

```tut:silent
@autoFunctorK @finalAlg
trait Bar[F[_]] {
  type T
  def a(i: Int): F[T]
}

implicit val tryInt = new Bar[Try] {
   type T = String
   def a(i: Int): Try[String] = Try(i.toString)
}

import Bar.autoDerive._
```

If you try to map this tryInt to a `Bar[Option]`, the `type T` of the `Bar[Option]` isn't refined.  That is, you can do

```tut:book
Bar[Option].a(3)
```
But you can't create a `Bar[Option]{ type T = String }` from the `tryInt` using `FunctorK`.

```tut:fail
val barOption: Bar[Option] { type T = String } = tryInt.mapK(fk)
```

However, there is also `mapK` function added to the companion object of the algebra which gives you more precise type.

```tut:book
val barOption: Bar[Option] { type T = String } = Bar.mapK(tryInt)(fk)
```

Also since the `FunctorK` (or `InvariantK`) instance uses a dependent type on the original interpreter, you may run into dependent type related issues. In those cases, this `mapK` (or `imapK`) on the companion object may give better result.
Here are two examples.

#### Cannot resolve implicit defined by the dependent type

```tut:silent
import cats.Show
import cats.implicits._

@autoFunctorK
trait Algebra[F[_]] {
  type TC[T]
  def a[T: TC](t: T): F[String]
}

object tryInt extends Algebra[Try] {
  type TC[T] = Show[T]
  def a[T: TC](t: T): Try[String] = Try(t.show)
}
```
`FunctorK.mapK` will result in unusable interpreter due to scalac's difficulty in resolving implicit based on dependent type.
```tut:fail
FunctorK[Algebra].mapK(tryInt)(fk).a(List(1,2,3))
```
The `mapK` on the companion will work fine.
```tut:book
Algebra.mapK(tryInt)(fk).a(List(1,2,3))
```

#### Cannot take in argument whose type is a dependent type
```tut:silent
@autoInvariantK
trait InvAlg[F[_]] {
  type T
  def a(i: F[T]): F[T]
}

object tryInt extends InvAlg[Try] {
  type T = String
  def a(i: Try[String]): Try[String] = i.map(_ + "a")
}
implicit val rk: Option ~> Try = λ[Option ~> Try](o => Try(o.get))

```
`InvariantK.imapK` will result in unusable interpreter because method `a`'s argument type is a dependent type on original interpreter.
```tut:fail
InvariantK[InvAlg].imapK(tryInt)(fk)(rk).a(Some("4"))
```
The `imapK` on the companion will work fine
```tut:book
InvAlg.imapK(tryInt)(fk)(rk).a(Some("4"))
```

### I am seeing diverging implicit expansion for type MyAlgebra[F]

If you see error likes the following when you try to summon a specific instance of `MyAlgebra`

> diverging implicit expansion for type MyAlgebra[F]
>
> [error] starting with method autoDeriveFromFunctorK in object MyAlgebra

It probably means that necessary implicit `MyAlgebra` instance and/or the corresponding `FunctionK` is missing in scope. 
