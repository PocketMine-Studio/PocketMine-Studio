<?php
namespace pocketmine {
    if (!function_exists('pocketmine\extension_loaded')) {
        function extension_loaded(string $name): bool {
            if ($name === 'pthreads') {
                return \extension_loaded('pmmpthread') || \extension_loaded('pthreads');
            }
            return \extension_loaded($name);
        }
    }
    if (!function_exists('pocketmine\phpversion')) {
        function phpversion(?string $extension = null) {
            if ($extension === 'pthreads' && !\extension_loaded('pthreads') && \extension_loaded('pmmpthread')) {
                return '4.2.30';
            }
            return \phpversion($extension);
        }
    }
}

namespace {
    if (!defined('PTHREADS_INHERIT_ALL')) define('PTHREADS_INHERIT_ALL', 0x111111);
    if (!defined('PTHREADS_INHERIT_NONE')) define('PTHREADS_INHERIT_NONE', 0);
    if (!defined('PTHREADS_INHERIT_INI')) define('PTHREADS_INHERIT_INI', 0x1);
    if (!defined('PTHREADS_INHERIT_CONSTANTS')) define('PTHREADS_INHERIT_CONSTANTS', 0x10);
    if (!defined('PTHREADS_INHERIT_CLASSES')) define('PTHREADS_INHERIT_CLASSES', 0x100);
    if (!defined('PTHREADS_INHERIT_FUNCTIONS')) define('PTHREADS_INHERIT_FUNCTIONS', 0x1000);
    if (!defined('PTHREADS_INHERIT_INCLUDES')) define('PTHREADS_INHERIT_INCLUDES', 0x10000);
    if (!defined('PTHREADS_INHERIT_COMMENTS')) define('PTHREADS_INHERIT_COMMENTS', 0x100000);
    if (!defined('PTHREADS_ALLOW_GLOBALS')) define('PTHREADS_ALLOW_GLOBALS', 0x1000000);

    if (\extension_loaded('pmmpthread') && !\extension_loaded('pthreads')) {
        if (!class_exists('Thread', false) && class_exists('pmmp\thread\Thread')) {
            abstract class Thread extends \pmmp\thread\Thread {}
        }
        if (!class_exists('Worker', false) && class_exists('pmmp\thread\Worker')) {
            abstract class Worker extends \pmmp\thread\Worker {}
        }
        if (!class_exists('Pool', false) && class_exists('pmmp\thread\Pool')) {
            class Pool extends \pmmp\thread\Pool {}
        }
        if (!interface_exists('Runnable', false) && interface_exists('pmmp\thread\Runnable')) {
            interface Runnable extends \pmmp\thread\Runnable {}
        }
        if (!class_exists('Threaded', false) && class_exists('pmmp\thread\ThreadSafe')) {
            class Threaded extends \pmmp\thread\ThreadSafe {}
        }
        if (!class_exists('Volatile', false) && class_exists('pmmp\thread\ThreadSafe')) {
            class Volatile extends \pmmp\thread\ThreadSafe {}
        }
        if (!class_exists('ThreadedArray', false) && class_exists('pmmp\thread\ThreadSafeArray')) {
            class ThreadedArray extends \pmmp\thread\ThreadSafeArray {}
        }
    }
}
