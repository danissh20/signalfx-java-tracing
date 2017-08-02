package com.datadoghq.trace.integration

import com.datadoghq.trace.DDSpanContext
import com.datadoghq.trace.DDTracer
import com.datadoghq.trace.SpanFactory
import io.opentracing.tag.StringTag
import io.opentracing.tag.Tags
import spock.lang.Specification

class SpanDecoratorTest extends Specification {

  def "adding span personalisation using Decorators"() {
    setup:
    def tracer = new DDTracer()
    def decorator = new AbstractDecorator() {

      @Override
      boolean afterSetTag(DDSpanContext context, String tag, Object value) {
        return super.afterSetTag(context, tag, value)
      }

    }
    decorator.setMatchingTag("foo")
    decorator.setMatchingValue("bar")
    decorator.setSetTag("newFoo")
    decorator.setSetValue("newBar")
    tracer.addDecorator(decorator)

    def span = SpanFactory.newSpanOf(tracer)
    new StringTag("foo").set(span, "bar")

    expect:
    span.getTags().containsKey("newFoo")
    span.getTags().get("newFoo") == "newBar"
  }

  def "override operation with OperationDecorator"() {

    setup:
    def tracer = new DDTracer()
    def span = SpanFactory.newSpanOf(tracer)
    tracer.addDecorator(new OperationDecorator())

    when:
    Tags.COMPONENT.set(span, component)

    then:
    span.getOperationName() == operationName

    where:
    component << OperationDecorator.MAPPINGS.keySet()
    operationName << OperationDecorator.MAPPINGS.values()


  }

  def "override operation with DBTypeDecorator"() {

    setup:
    def tracer = new DDTracer()
    def span = SpanFactory.newSpanOf(tracer)
    tracer.addDecorator(new DBTypeDecorator())

    when:
    Tags.DB_TYPE.set(span, type)

    then:
    span.getOperationName() == type + ".query"
    span.context().getSpanType() == "sql"


    when:
    Tags.DB_TYPE.set(span, "mongo")

    then:
    span.getOperationName() == "mongo.query"
    span.context().getSpanType() == "mongodb"


    where:
    type = "foo"


  }

  def "DBStatementAsResource should not interact on Mongo queries"() {
    setup:
    def tracer = new DDTracer()
    def span = SpanFactory.newSpanOf(tracer)
    tracer.addDecorator(new DBStatementAsResourceName())

    when:
    span.setResourceName("not-change-me")
    Tags.COMPONENT.set(span, "java-mongo")
    Tags.DB_STATEMENT.set(span, something)

    then:
    span.getResourceName() == "not-change-me"


    when:
    span.setResourceName("change-me")
    Tags.COMPONENT.set(span, "other-contrib")
    Tags.DB_STATEMENT.set(span, something)

    then:
    span.getResourceName() == something

    where:
    something = "fake-query"
  }
}
