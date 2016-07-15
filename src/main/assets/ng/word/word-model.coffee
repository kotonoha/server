angular.module("kotonoha").controller "WordCon", ($context, wordProvider) ->
  $context.word = wordProvider.provide()

