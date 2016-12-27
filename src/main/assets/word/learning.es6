let now = new Date().getTime();

let millisInDay = 1000 * 60 * 60 * 24;

function process(objs) {
  let l = [];
  for (let i = 0; i < objs.length; ++i) {
    let x = objs[i];
    l.push([i - 1, x]);
  }
  return l;
}

function process2(objs) {
  let len = objs.length - 1;
  let l = [];
  for (let i = 0; i < len; ++i) {
    l.push([new Date(now - i * millisInDay), objs[len - i]]);
  }
  return l;
}

function display(stats) {
  drawNext10(stats.next);
  drawLast(stats.last)
}

function drawNext10(dates) {
  let data = [
    process(dates.ready),
    process(dates.bad),
    process(dates.readyNa),
    process(dates.badNa)
  ];

  $.jqplot("next10", data, {
    title: "Scheduled count for next 10 days",
    stackSeries: true,
    seriesDefaults: {
      renderer: $.jqplot.BarRenderer,
      rendererOptions: {barWidth: 20},
      pointLabels: {show: true}
    },
    axes: {
      xaxis: {
        min: -2,
        max: 10,
        ticks: [
          [-2, ""],
          [-1, 'Ready'],
          [0, '24h'],
          [2, '3 days'],
          [4, '5 days'],
          [6, '7 days'],
          [8, '9 days'],
          [10, ""]
        ]
      }
    },
    legend: {
      show: true,
      placement: "outside",
      location: "e",
      labels: ['Good', 'Bad', 'Good, but not available now', 'Bad, but not available now']
    }
  })
}

function drawLast(count) {
  $.jqplot("lastmonth", [process2(count)], {
    title: "Repetition counts for last month",
    seriesDefaults: {
      renderer: $.jqplot.BarRenderer,
      rendererOptions: {barWidth: 12},
      pointLabels: {show: true},
    },
    axes: {
      xaxis: {
        renderer: $.jqplot.DateAxisRenderer,
        tickOptions: {formatString: "%d.%m"}
      }
    }
  })
}

angular.module('kotonoha').controller("Learning", ["$scope", "$http", ($scope, $http) => {
  $http.get('../api/stats').then((o) => {
    $scope.stats = o.data;
    setTimeout(() => display(o.data), 0);
  });
}]);
