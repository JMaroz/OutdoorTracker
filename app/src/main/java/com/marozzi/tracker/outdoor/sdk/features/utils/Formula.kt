package com.marozzi.tracker.outdoor.sdk.features.utils

import java.math.BigDecimal
import kotlin.math.*

/**
 * Created by amarozzi on 2020-07-19
 */
object Formula {

    object WalkingFormula {
        /**
         * ASCM running from speed to grade
         *
         * @param speedKmH  [km/h]
         * @param gradeperc [%]
         * @return VO2 [ml · kg−1 · min−1]
         */
        fun calculateVO2Ascm(speedKmH: Double, gradeperc: Double): Double {
            val vo2: Double
            val nVelLow = 5.0
            val nVelHigh = 7.0
            val lowVO2 = nVelLow * 5.0 / 3.0 + nVelLow * gradeperc * 0.3 + 3.5
            val highVO2 = nVelHigh * 10 / 3 + nVelHigh * gradeperc * 0.15 + 3.5
            val MM = (highVO2 - lowVO2) / (nVelHigh - nVelLow)
            val NN = lowVO2 - MM * nVelLow
            vo2 = if (speedKmH <= nVelLow) {
                speedKmH * 1.666666666 + speedKmH * gradeperc * 0.3 + 3.5
            } else if (speedKmH >= nVelHigh) {
                speedKmH * 3.333333333 + speedKmH * gradeperc * 0.15 + 3.5
            } else {
                speedKmH * MM + NN
            }
            var bd = BigDecimal(vo2)
            bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP)
            return bd.toDouble()
        }
    }

    /**
     * @author mguidi
     */
    object RunningFormula {
        /**
         * Calculare speed from steps and duration
         *
         * @param step
         * @param duration [ms]
         * @return speed [m/s]
         */
        fun calculateSpeed(step: Int, duration: Long): Double {
            val a = 0.0189
            val b = -0.1868
            val c = 0.7816
            val d = 0.2398 - step / (duration / 60000.0) / 120.0
            val cubic = Cubic()
            return try {
                cubic.solve(a, b, c, d)
                if (cubic.x1 < 0) {
                    return 0.0
                }
                var bd = BigDecimal(cubic.x1)
                bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP)
                bd.toDouble()
            } catch (e: RuntimeException) {
                0.0
            }
        }
    }

    object CyclingFormula {

        /**
         * Calculate power consumption
         *
         *
         * http://www.gribble.org/cycling/power_v_speed.html
         *
         * @param weight [kg]
         * @param slope  [%] 0 - 1
         * @param speed  [m/s]
         * @return power [w]
         */
        @Deprecated("")
        fun calculatePower(weight: Double, slope: Double, speed: Double): Double {
            var weight = weight
            weight += 13.0 //bike weight
            val fgravity = 9.8067 * sin(atan(slope)) * weight
            val frolling = 9.8067 * cos(atan(slope)) * weight * 0.005
            val fdrag = 0.5 * 0.321 * 1.226 * speed * speed
            val fresist = fgravity + frolling + fdrag
            val wattAtWhell = fresist * speed
            val wattAtPedal = (1.0 - 3.0 / 100.0).pow(-1.0) * wattAtWhell
            if (wattAtPedal > 1600.0) return 1600.0 else if (wattAtPedal < 0.0) return 0.0
            var bd = BigDecimal(wattAtPedal)
            bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP)
            return bd.toDouble()
        }

        /**
         * Calculate power consumption
         *
         *
         * http://www.gribble.org/cycling/power_v_speed.html
         *
         * @param weight [kg]
         * @param slope  [%] 0 - 1
         * @param speed  [m/s]
         * @return power [w]
         */
        fun calculatePowerStep(weight: Double, slope: Double, speed: Double): Double {
            var weight = weight
            weight += 13.0 //bike weight
            val fgravity = 9.8067 * sin(atan(slope)) * weight
            val frolling = 9.8067 * cos(atan(slope)) * weight * 0.005
            val fdrag = 0.5 * 0.321 * 1.226 * speed * speed
            val fresist = fgravity + frolling + fdrag
            val wattAtWhell = fresist * speed
            val wattAtPedal = Math.pow(1.0 - 3.0 / 100.0, -1.0) * wattAtWhell
            if (wattAtPedal > 1600.0) return 1600.0 else if (wattAtPedal < 0.0) return 0.0
            return wattAtPedal
        }

        /**
         * Calculate calories from power and duration
         *
         * @param power    [w]
         * @param duration [sec]
         * @return calories [kcal]
         */
        @Deprecated("")
        fun calculateCalories(power: Double, duration: Int): Int {
            val calories = 0.78 * (power * duration.toDouble() / 1000.0) / 1.11631
            var bd = BigDecimal(calories)
            bd = bd.setScale(0, BigDecimal.ROUND_HALF_EVEN)
            return bd.toInt()
        }

        fun calculateCaloriesForBike(power: Double, duration: Int): Int {
            var calories = power * duration.toDouble() / 1000.0 / 1.11631
            calories *= (0.78 + getDouble(0.04, 0.0))
            var bd = BigDecimal(calories)
            bd = bd.setScale(0, BigDecimal.ROUND_HALF_UP)
            return bd.toInt()
        }

        /**
         * Calculate calories from power and duration
         *
         * @param power    [w]
         * @param duration [sec]
         * @return calories [kcal]
         */
        fun calculateCaloriesStep(power: Double, duration: Int): Double {
            return power * duration.toDouble() / 1000.0 / 1.11631
        }
    }

    /**
     * http://en.wikipedia.org/wiki/Metabolic_equivalent
     *
     * @param VO2 [ml · kg−1 · min−1]
     * @return mets [kcal·kg−1·h−1]
     */
    fun calculateMets(VO2: Double): Double {
        val mets = VO2 / 3.5
        var bd = BigDecimal(mets)
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP)
        return bd.toDouble()
    }

    /**
     * http://en.wikipedia.org/wiki/Metabolic_equivalent
     *
     * @param mets       [kcal·kg−1·h−1]
     * @param duration   [sec]
     * @param userWeight [kg]
     * @return calories  [kcal]
     */
    fun calculateCalories(mets: Double, duration: Int, userWeight: Double): Int {
        val calories = mets * userWeight * duration.toDouble() / 3600.0
        var bd = BigDecimal(calories)
        bd = bd.setScale(0, BigDecimal.ROUND_HALF_UP)
        return bd.toInt()
    }

    /**
     * http://en.wikipedia.org/wiki/Metabolic_equivalent
     *
     * @param calories   [kcal]
     * @param duration   [sec]
     * @param userWeight [kg]
     * @return mets [kcal·kg−1·h−1]
     */
    fun calculateMetsFromCaloriesAndDuration(calories: Int, duration: Int, userWeight: Double): Double {
        val mets = 3600.0 * calories / (userWeight * duration)
        var bd = BigDecimal(mets)
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP)
        return bd.toDouble()
    }

    /**
     * http://en.wikipedia.org/wiki/Metabolic_equivalent
     *
     * @param calories   [kcal]
     * @param duration   [sec]
     * @param userWeight [kg]
     * @return mets [kcal·kg−1·h−1]
     */
    fun calculateMetsFromCaloriesAndDuration(calories: Double, duration: Int, userWeight: Double): Double {
        return 3600.0 * calories / (userWeight * duration)
    }

    /**
     * @param speed    [m/s]
     * @param duration [ms]
     * @return distance [m]
     */
    fun calculateDistance(speed: Double, duration: Long): Double {
        val distance = speed * (duration / 1000.0)
        var bd = BigDecimal(distance)
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP)
        return bd.toDouble()
    }

    /**
     * @param speed [km/h]
     * @return pace [min/km]
     */
    fun calculatePace(speed: Double): Double {
        if (speed == 0.0) {
            return 0.0
        }
        val pace = 60.0 / speed
        var bd = BigDecimal(pace)
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP)
        return bd.toDouble()
    }

    /**
     * Convert speed from m/s to km/h
     *
     * @param speed [m/s]
     * @return speed [km/h]
     */
    fun convertSpeedFromMsToKmh(speed: Double): Double {
        var bd: BigDecimal
        try {
            bd = BigDecimal(speed * 3.6)
            bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP)
        } catch (e: NumberFormatException) {

            /*
        Exception Not a number
         */
            bd = BigDecimal(0)
            bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP)
        }
        return bd.toDouble()
    }

    /**
     * Convert speed from m to mi
     *
     * @param meters [m]
     * @return miles [mi]
     */
    fun convertMetersFromMetersToMiles(meters: Double): Double {
        return meters * 0.000621371
    }

    /**
     * Convert speed from km/h to m/s
     *
     * @param speed [km/h]
     * @return speed [m/s]
     */
    fun convertSpeedFromKmhToMs(speed: Double): Double {
        var bd = BigDecimal(speed / 3.6)
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP)
        return bd.toDouble()
    }

    fun convertPaceKmToPaceMiles(pacekm: Double): Double {
        return pacekm / 0.621371
    }

    fun convertPaceKmToPaceMiles(pacekm: Long): Long {
        return (pacekm.toDouble() / 0.621371).toLong()
    }

    /**
     * @param diameterInch [inch]
     * @return circumference [cm]
     */
    fun convertDiameterToCircCm(diameterInch: Double): Double {
        return diameterInch * Math.PI.toFloat() * 2.54f / 100f
    }

    fun round(value: Double, places: Int): Double {
        require(places >= 0)
        var value = value
        val factor = 10.0.pow(places.toDouble()).toLong()
        value *= factor
        val tmp = value.roundToInt()
        return tmp.toDouble() / factor
    }

    fun interpolation(x1: Double, x2: Double, y1: Double, y2: Double, x: Double): Double {
        return (x - x1) / (x2 - x1) * (y2 - y1) + y1
    }

    /**
     * @param weight [kg]
     * @param watt   [watt]
     * @return
     */
    private fun calculateVO2(weight: Double, watt: Int): Double {
        return 6.12 * watt * (1.8 / weight) - 7
    }

    /**
     * User this only for bike mode in power loop control
     *
     *
     * double coeffAerodinamic = 0.021;
     * double coeffRolling = 0.01;
     * double gravityAcc = 9.81;
     * See https://mwcloud.atlassian.net/browse/SPIA-240
     * double a = coeffAerodinamic * gravityAcc;
     * double c = coeffRolling * gravityAcc * weight;
     * double d = -power
     *
     * @param weight [kg] user + bike
     * @param power  [watt]
     * @return -1 if there is error
     */
    fun ambrosiniSpeed(weight: Double, power: Double): Double {
        val a = 0.20601
        val c = 0.0981 * weight
        val d = -power
        val p = c / a
        val q = d / a
        val value1 = -q / 2.0 + sqrt(q.pow(2.0) / 4.0 + p.pow(3.0) / 27.0)
        val sign1 = sign(value1).toInt()
        val value2 = -q / 2.0 - sqrt(q.pow(2.0) / 4.0 + p.pow(3.0) / 27.0)
        val sign2 = sign(value2).toInt()
        val U = abs(value1).pow(1.0 / 3.0)
        val V = abs(value2).pow(1.0 / 3.0)
        return sign1 * U + sign2 * V
    }

    /**
     * @param userThresholdPower
     * @param wattPerc
     * @param userWeightBike (user + bike weight)
     * @param grade              P = Peso.
     * H = Pendenza.
     * A = Attrito (pari a 0.01).
     * K = Coefficiente aereodinamico (pari a 0.021).
     * V = Velocita.
     * G = Acelerazione di gravita (pari a 9.81).
     *
     *
     * potenza = (P*(H+A) + K*V*V) * V * G
     * G*K*V*V*V + P*G*(H+A)*V - potenza = 0
     *
     *
     * da qui segue che :
     *
     *
     * a = G*K
     * b = 0
     * c = P*G*(H+A)
     * d = -potenza
     *
     *
     * Volendo riportare l'equazione alla forma y^3+py+q=0  [essendo b = 0 ] , dove
     *
     *
     * p = c/a  & q = d/a
     *
     *
     * p = k/P*(H + A)
     * q =  -potenza / K*G
     * @return m/s
     */
    fun CalcSpeedByPowerAmbrosini(userThresholdPower: Int, wattPerc: Int, userWeightBike: Double, grade: Double): Double {
        var grade = grade
        val a = 0.24525 //9,81 * 0,025 == G*K
        val power = wattPerc * userThresholdPower / 100.0
        if (grade != 0.0) grade /= 100.0
        val c = 9.81 * userWeightBike * (grade + 0.003) //P*G*(H+A) = peso *9.81 * (pendenza + attrito)
        val d = -power
        val p = c / a
        val q = d / a
        val val1 = -q / 2.0
        val val2 = q.pow(2.0) / 4.0 + p.pow(3.0) / 27.0
        val val3 = sqrt(val2)
        val value1 = val1 + val3
        val sign1 = sign(value1).toInt()
        val U = abs(value1).pow(1.0 / 3.0)
        val value2 = val1 - val3
        val sign2 = sign(value2).toInt()
        val V = abs(value2).pow(1.0 / 3.0)
        return sign1 * U + sign2 * V
    }


    fun getDouble(rangeMax: Double, rangeMin: Double): Double {
        val r = java.util.Random()
        return rangeMin + (rangeMax - rangeMin) * r.nextDouble()
    }

    fun getInt(rangeMax: Int, rangeMin: Int): Int {
        val r = java.util.Random()
        return Math.round((rangeMin + (rangeMax - rangeMin) * r.nextDouble()).toFloat())
    }
}