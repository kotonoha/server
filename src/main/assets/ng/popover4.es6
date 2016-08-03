/**
 * @author eiennohito
 * @since 2016/08/03
 */

let popover4 = angular.module('ui.popover4', []);

popover4.directive('popover4', [() => {
    return {
      restrict: 'A',
      template: "<div ng-transclude></div>",
      controller: () => {
        return {
          pobj: null,
          $onDestroy: function() { if (this.pobj != null) this.pobj.popover('dispose'); }
        };
      },
      transclude: true,
      link: (scope, elem, attr, ctrl, transclude) => {
        /*if (!attr.enabled) {
          elem.remove();
          return
        }*/

        let parent = elem.parent();
        let po = parent.popover({
          container: "body",
          content: elem,
          html: true,
          trigger: "manual",
          placement: "bottom"
        });

        po.on('click', () => {
          po.popover('toggle');
        });

        ctrl.pobj = po;
      }
    }
}]);






