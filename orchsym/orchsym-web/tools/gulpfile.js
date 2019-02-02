var gulp = require('gulp'),
    htmlmini = require('gulp-html-minify'),
    useref = require('gulp-useref'),
    uglify = require('gulp-uglify'),
    csso = require('gulp-csso'),
    filter = require('gulp-filter'),
    RevAll = require('gulp-rev-all'),
    del = require('del'),
    contentIncluder = require('gulp-content-includer');

gulp.task('del', function () {
    return del('orchsym-web/*.html');                               // 构建前先删除dist文件里的旧版本
})

gulp.task('html', function () {
  var jsFilter = filter('**/*.js',{restore:true}),
      cssFilter = filter('**/*.css',{restore:true}),
      htmlFilter = filter(['**/*.html'],{restore:true});
  return gulp.src('WEB-INF/pages/*.html')
        .pipe(useref())
        .pipe(jsFilter)
        // .pipe(uglify())
        .pipe(jsFilter.restore)
        .pipe(cssFilter)
        .pipe(csso())
        .pipe(cssFilter.restore)
        .pipe(RevAll.revision({                 // 生成版本号
            dontRenameFile: ['.html'],          // 不给 html 文件添加版本号
            dontUpdateReference: ['.html']      // 不给文件里链接的html加版本号
        }))
        .pipe(htmlFilter)                       // 过滤所有html
        .pipe(htmlmini())                       // 压缩html
        .pipe(htmlFilter.restore)
        .pipe(contentIncluder({
          includerReg: /<jsp:include\s+page="([^"]+)"\s*\/>/g
        }))
        .pipe(gulp.dest('orchsym-web'))
})

gulp.task('default', gulp.series('del', 'html'))
