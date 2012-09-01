process = (objs) ->
  ([o.idx, o.count] for o in objs)

datesCallback = (dates) ->
  $.jqplot('chart2', [process(dates)], {
    title: "Scheduled count for next 10 days",
    seriesDefaults: {
      renderer: $.jqplot.BarRenderer,
      rendererOptions: {
        barWidth: 25
      },
      pointLabels: {show: true}
    }
  })
  return

$(document).ready ->
  $.ajax({
    url: "../api/stats/personal/future_reps",
    dataType: "json",
    success: datesCallback
  })
