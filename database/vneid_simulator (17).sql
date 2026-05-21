-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: May 21, 2026 at 12:04 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `vneid_simulator`
--

-- --------------------------------------------------------

--
-- Table structure for table `citizens`
--

CREATE TABLE `citizens` (
  `cccd_number` varchar(12) NOT NULL,
  `phone_number` varchar(15) DEFAULT NULL,
  `password_hash` varchar(255) NOT NULL,
  `passcode_hash` varchar(255) NOT NULL,
  `full_name` varchar(255) NOT NULL,
  `dob` date DEFAULT NULL,
  `gender` varchar(10) DEFAULT NULL,
  `account_status` varchar(20) DEFAULT 'ACTIVE',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `email` varchar(255) DEFAULT NULL,
  `firebase_uid` varchar(255) DEFAULT NULL,
  `firebase_email` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `citizens`
--

INSERT INTO `citizens` (`cccd_number`, `phone_number`, `password_hash`, `passcode_hash`, `full_name`, `dob`, `gender`, `account_status`, `created_at`, `email`, `firebase_uid`, `firebase_email`) VALUES
('001099012345', '0912999888', 'vneid123', '123456', 'Nguyen Van Binh (Citizen)', '1999-01-01', 'Nam', 'ACTIVE', '2026-05-20 11:04:03', 'binh.nv@mock.vn', NULL, NULL),
('001201000011', '0922000011', '123456', '123456', 'Lý Nhã Kỳ', '1989-09-09', 'Nữ', 'ACTIVE', '2026-05-20 11:04:03', 'ky.ly@mock.vn', NULL, NULL),
('079090000002', '0912000002', '123456', '123456', 'Cán bộ địa chính', '1985-05-15', 'Nam', 'ACTIVE', '2026-05-20 11:04:03', 'land.officer@mock.vn', NULL, NULL),
('079090000003', '0912000003', '123456', '123456', 'Cán bộ Thuế', '1988-08-20', 'Nữ', 'ACTIVE', '2026-05-20 11:04:03', 'tax.officer@mock.vn', NULL, NULL),
('079090012345', '0911223344', 'vneid123', '123456', 'Le Hai Dang (Admin)', '1990-01-01', 'Nam', 'ACTIVE', '2026-05-20 11:04:03', 'dang.lh@mock.vn', NULL, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `otp_requests`
--

CREATE TABLE `otp_requests` (
  `id` bigint(20) NOT NULL,
  `cccd_number` varchar(12) NOT NULL,
  `otp_code` varchar(6) NOT NULL,
  `email` varchar(255) NOT NULL,
  `expires_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `is_used` tinyint(1) DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `qr_login_sessions`
--

CREATE TABLE `qr_login_sessions` (
  `qr_token` varchar(255) NOT NULL,
  `cccd_number` varchar(12) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'PENDING',
  `expires_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `citizens`
--
ALTER TABLE `citizens`
  ADD PRIMARY KEY (`cccd_number`),
  ADD UNIQUE KEY `phone_number` (`phone_number`);

--
-- Indexes for table `otp_requests`
--
ALTER TABLE `otp_requests`
  ADD PRIMARY KEY (`id`),
  ADD KEY `cccd_number` (`cccd_number`);

--
-- Indexes for table `qr_login_sessions`
--
ALTER TABLE `qr_login_sessions`
  ADD PRIMARY KEY (`qr_token`),
  ADD KEY `cccd_number` (`cccd_number`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `otp_requests`
--
ALTER TABLE `otp_requests`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `otp_requests`
--
ALTER TABLE `otp_requests`
  ADD CONSTRAINT `otp_requests_ibfk_1` FOREIGN KEY (`cccd_number`) REFERENCES `citizens` (`cccd_number`) ON DELETE CASCADE;

--
-- Constraints for table `qr_login_sessions`
--
ALTER TABLE `qr_login_sessions`
  ADD CONSTRAINT `qr_login_sessions_ibfk_1` FOREIGN KEY (`cccd_number`) REFERENCES `citizens` (`cccd_number`) ON DELETE SET NULL;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
