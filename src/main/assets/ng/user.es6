let mod = angular.module("kotonoha");

mod.controller('UserProfile', ["$scope", "UserCallback", (scope, userCb) => {
  scope.user = window.currentUserObj;

  scope.save = () => {
    if (scope.basicInfo.$invalid) {
      console.log("basic info form is invalid");
      return
    }

    let u = scope.user;
    scope.basicMessage = null;
    let cmd = {
      cmd: "save-user",
      data: {
        username: u.username,
        firstName: u.firstName,
        lastName: u.lastName,
        locale: u.locale,
        timezone: u.timezone,
        languages: selectedLangs()
      }
    };

    userCb(cmd).then(resp => {
      scope.$apply(() => {
        if (resp.status === "ok") {
          scope.reset(scope.basicInfo);
          scope.basicMessage = {
            content: "Information was saved",
            style: "success"
          }
        } else {
          scope.basicMessage = {
            content: resp.message,
            style: "error"
          }
        }
      });
    })
  };

  scope.recomputeTest = () => {
    userCb({
      cmd: "recompute",
      data: {
        locale: scope.user.locale,
        timezone: scope.user.timezone
      }
    }).then(data => scope.$apply(() => scope.user.timezoneTest = data))
  };

  function selectedLangs() {
    return scope.user.languages.filter(l => l.selected).map(l => l.code);
  }

  scope.updateJmdict = () => {
    const codes = selectedLangs();
    userCb({
      cmd: "update-jmdict",
      data: codes
    }).then(jmd => scope.$apply(() => scope.user.jmdict = jmd));
  };

  scope.isEng = (lang) => lang.code === "eng";

  scope.reset = function (form) {
    if (form) {
      form.$setPristine();
      form.$setUntouched();
    }
  };

  scope.reset();
}]);


mod.directive('ngUsername', ['$q','UserCallback', function ($q, uc) {
  return {
    'require': 'ngModel',
    link: (scope, elm, attrs, ctrl) => {
      ctrl.$asyncValidators.username = (modelValue, viewValue) => {
        if (ctrl.$isEmpty(modelValue)) {
          return $q.reject("Username can not be empty");
        }

        let d = $q.defer();

        uc({
          cmd: 'check-username',
          data: modelValue
        }).then(o => {
          if (o.status === "ok") {
            return d.resolve();
          } else {
            d.reject(d.message);
          }
        });

        return d.promise;
      }
    }
  }
}]);

mod.directive('bsValid', function() {
  return {
    'require': 'ngModel',
    link: (scope, elm, attrs, ctrl) => {
      let type = attrs['bsValid'];
      if (type === undefined) {
        type = 'danger';
      }

      const successName = 'form-control-success';
      const failname = `form-control-${type}`;
      const hasSuccess = 'has-success';
      const hasFailure = `has-${type}`;

      let parent = jQuery(elm).closest('.form-group');

      ctrl.$viewChangeListeners.push(() => {
        if (ctrl.$valid) {
          elm.removeClass(failname);
          elm.addClass(successName);
          parent.addClass(hasSuccess);
          parent.removeClass(hasFailure);
        } else {
          elm.addClass(failname);
          elm.removeClass(successName);
          parent.addClass(hasFailure);
          parent.removeClass(hasSuccess);
        }
      });

    }
  }
});
