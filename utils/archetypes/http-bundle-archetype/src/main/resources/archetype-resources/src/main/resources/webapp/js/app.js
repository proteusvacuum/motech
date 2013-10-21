(function () {
    'use strict';

    /* App Module */

    angular.module('hello-world', ['motech-dashboard', 'ngCookies', 'bootstrap', 'helloWorldService']).config(['$routeProvider',
        function ($routeProvider) {
            $routeProvider.
                when('/hello-world', {templateUrl: '../${artifactId}/resources/partials/say-hello.html', controller: 'HelloWorldController'}).
                otherwise({redirectTo: '/hello-world'});
    }]);
}());