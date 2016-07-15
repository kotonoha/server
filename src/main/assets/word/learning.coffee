now = new Date().getTime()

millsInDay = 1000 * 60 * 60 * 24

process = (objs) ->
  ([i - 1, o] for o, i in objs)


process2 = (objs) ->
  len = objs.length - 1
  [new Date(now - i * millsInDay), objs[len - i]] for i in [0...len]

drawNext10 = (dates) ->
  $.jqplot 'next10', [process(dates.ready), process(dates.bad), process(dates.readyNa), process(dates.badNa)],
    title: "Scheduled count for next 10 days",
    stackSeries: true,
    seriesDefaults:
      renderer: $.jqplot.BarRenderer,
      rendererOptions:
        barWidth: 20
      pointLabels:
        {show: true}
    axes:
      xaxis:
        min: -2,
        max: 10,
        ticks: [
          [-2, ""]
          [-1, 'Ready'],
          [0, '24h'],
          [2, '3 days'],
          [4, '5 days'],
          [6, '7 days'],
          [8, '9 days']
          [10, ""]
        ]
    legend:
      show: true
      placement: 'outside'
      location: 'e'
      labels: ['Good', 'Bad', 'Good, but not available now', 'Bad, but not available now']

drawLast = (count) ->
  $.jqplot 'lastmonth', [process2(count)],
    title: "Repetition counts for last month"
    seriesDefaults:
      renderer: $.jqplot.BarRenderer
      rendererOptions:
        barWidth: 12
      pointLabels:
        show: true
    axes:
      xaxis:
        renderer: $.jqplot.DateAxisRenderer
        tickOptions:
          formatString: '%d.%m'

display = (stats) ->
  drawNext10(stats.next)
  drawLast(stats.last)

window.Learning = ($scope, $http) ->
  $http.get('../api/stats').success (o) ->
    $scope.stats = o
    fnc = -> display(o)
    setTimeout fnc, 0
    return

