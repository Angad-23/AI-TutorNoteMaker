-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: triviumdb
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `tutor_users`
--

DROP TABLE IF EXISTS `tutor_users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tutor_users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `full_name` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` varchar(255) DEFAULT NULL,
  `username` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKsjf1aw5lki6m2n4ktg6tupqqa` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tutor_users`
--

LOCK TABLES `tutor_users` WRITE;
/*!40000 ALTER TABLE `tutor_users` DISABLE KEYS */;
INSERT INTO `tutor_users` VALUES (1,'2026-05-24 08:14:05.620430','angaddhiman10@gmail.com','Angad Dhiman','1234','TUTOR','Angad'),(2,'2026-05-24 16:27:56.833381','suraj.singh@triviumedu.com','Suraj Singh','$2a$12$4v7gcgmv5m3WBDhADejaL.wwvMZnHqhdflpakjLjr3diyCXHpoEAy','TUTOR','suraj'),(3,'2026-05-24 18:35:44.511733','tutor1@abc.com','Tutor_1','$2a$10$sJko6j7/kRucqboHSoQkKuX2UBXO/Etrmw5.JaAwlimweNX0qgeYu','TUTOR','tutor_1'),(4,'2026-05-24 20:36:51.659265','tutor8@abc.com','Tutor 8','$2a$10$SjL0wYr8mStgCZES1F6/L.4tmDsrXsqPA/QFYqb/LfgeTZS2Qq7B2','TUTOR','tutor-8'),(5,'2026-06-02 14:11:21.736707','user1@gmail.com','User 1','$2a$10$q4lCtHQ9.66JQ5HCBsLvAujenRBnp3RxYC4DbbDZuJDvvzxMfWMG6','TUTOR','user_1'),(6,'2026-06-16 19:55:37.492301','crackytutor@abc.com','Crack Tutor','$2a$10$xOqw/evONY89ajLrqXCNCOC2dz54M90t1VGerPHg7W1oQMRZRZI36','TUTOR','cracky'),(7,'2026-06-19 07:28:03.317905','andy@abc.com','Andy','$2a$10$F56FLMcLoUYW.HbN1fvUreaKbPM32fklbPJfH.Hd3ONkcaRON1bb2','TUTOR','andy'),(8,'2026-06-19 14:05:29.216469','tim@eurekii.com','Tim','$2a$10$rixA8wq5gGIYSu23kGEH0.mdQgx01MOl8SPh9kaXdFZWVfqFrkGLu','TUTOR','tim');
/*!40000 ALTER TABLE `tutor_users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-23 18:02:13
