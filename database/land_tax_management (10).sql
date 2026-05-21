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
-- Database: `land_tax_management`
--

-- --------------------------------------------------------

--
-- Table structure for table `accounts`
--

CREATE TABLE `accounts` (
  `account_id` int(11) NOT NULL,
  `citizen_id` int(11) NOT NULL,
  `role_id` int(11) NOT NULL,
  `account_status` varchar(20) DEFAULT 'ACTIVE',
  `status_note` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `accounts`
--

INSERT INTO `accounts` (`account_id`, `citizen_id`, `role_id`, `account_status`, `status_note`) VALUES
(1, 1, 1, 'ACTIVE', NULL),
(2, 2, 3, 'ACTIVE', NULL),
(3, 3, 4, 'ACTIVE', NULL),
(4, 4, 2, 'ACTIVE', NULL),
(5, 5, 2, 'ACTIVE', NULL),
(6, 6, 2, 'ACTIVE', NULL),
(7, 7, 2, 'ACTIVE', NULL),
(8, 8, 2, 'ACTIVE', NULL),
(9, 9, 2, 'ACTIVE', NULL),
(10, 10, 2, 'ACTIVE', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `areas`
--

CREATE TABLE `areas` (
  `area_id` int(11) NOT NULL,
  `district_code` varchar(20) NOT NULL,
  `ward_code` varchar(20) NOT NULL,
  `street_name` varchar(255) DEFAULT NULL,
  `position_level` int(11) NOT NULL,
  `land_quota` decimal(10,2) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `areas`
--

INSERT INTO `areas` (`area_id`, `district_code`, `ward_code`, `street_name`, `position_level`, `land_quota`) VALUES
(1, 'D01', 'W01', 'Phố Huế', 1, 100.00),
(2, 'D01', 'W02', 'Hàng Bài', 1, 100.00),
(3, 'D02', 'W03', 'Kim Mã', 2, 120.00),
(4, 'D02', 'W04', 'Đội Cấn', 2, 120.00),
(5, 'D03', 'W05', 'Dịch Vọng', 3, 150.00),
(6, 'D03', 'W06', 'Mai Dịch', 3, 150.00),
(7, 'D04', 'W07', 'Nguyễn Trãi', 4, 180.00),
(8, 'D04', 'W08', 'Khương Trung', 4, 180.00),
(9, 'D05', 'W09', 'Tây Mỗ', 4, 200.00),
(10, 'D05', 'W10', 'Mễ Trì', 4, 200.00);

-- --------------------------------------------------------

--
-- Table structure for table `citizen_local`
--

CREATE TABLE `citizen_local` (
  `citizen_id` int(11) NOT NULL,
  `cccd_number` varchar(12) NOT NULL,
  `full_name` varchar(100) NOT NULL,
  `phone_number` varchar(20) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `citizen_local`
--

INSERT INTO `citizen_local` (`citizen_id`, `cccd_number`, `full_name`, `phone_number`, `email`) VALUES
(1, '079090012345', 'Lê Hải Đăng', '0911223344', 'admin.dang@landtax.vn'),
(2, '079090000003', 'Cán Bộ Thuế', '0912000003', 'tax.officer@landtax.vn'),
(3, '079090000002', 'Cán Bộ Địa Chính', '0912000002', 'land.officer@landtax.vn'),
(4, '001099012345', 'Nguyễn Văn Bình', '0912999888', 'binh.nv@citizen.vn'),
(5, '001201000011', 'Lý Nhã Kỳ', '0922000011', 'ky.ln@citizen.vn'),
(6, '001122334456', 'Phạm Xuân Ẩn', '0933111222', 'an.px@citizen.vn'),
(7, '001122334457', 'Vũ Trọng Phụng', '0944111222', 'phung.vt@citizen.vn'),
(8, '001122334458', 'Hồ Xuân Hương', '0955111222', 'huong.hx@citizen.vn'),
(9, '001122334459', 'Đinh Bộ Lĩnh', '0966111222', 'linh.db@citizen.vn'),
(10, '001122334460', 'Trần Hưng Đạo', '0977111222', 'dao.th@citizen.vn');

-- --------------------------------------------------------

--
-- Table structure for table `complaints`
--

CREATE TABLE `complaints` (
  `complaint_id` int(11) NOT NULL,
  `citizen_id` int(11) NOT NULL,
  `record_id` int(11) DEFAULT NULL,
  `content` text NOT NULL,
  `status` varchar(20) DEFAULT 'PENDING',
  `response_note` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `land_owners`
--

CREATE TABLE `land_owners` (
  `ownership_id` int(11) NOT NULL,
  `citizen_id` int(11) NOT NULL,
  `land_parcel_id` int(11) NOT NULL,
  `ownership_type` varchar(50) NOT NULL,
  `ownership_percentage` decimal(5,2) DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT 1,
  `assigned_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `land_owners`
--

INSERT INTO `land_owners` (`ownership_id`, `citizen_id`, `land_parcel_id`, `ownership_type`, `ownership_percentage`, `is_active`, `assigned_at`) VALUES
(1, 4, 1, 'MAIN', NULL, 1, '2026-05-20 16:47:09'),
(2, 5, 2, 'MAIN', NULL, 1, '2026-05-20 16:47:09'),
(3, 6, 3, 'MAIN', NULL, 1, '2026-05-20 16:47:09'),
(4, 7, 4, 'MAIN', NULL, 1, '2026-05-20 16:47:09'),
(5, 8, 5, 'MAIN', NULL, 1, '2026-05-20 16:47:09'),
(6, 9, 6, 'MAIN', NULL, 1, '2026-05-20 16:47:09'),
(7, 10, 7, 'MAIN', NULL, 1, '2026-05-20 16:47:09'),
(8, 4, 8, 'MAIN', NULL, 1, '2026-05-20 16:47:09'),
(9, 5, 9, 'MAIN', NULL, 1, '2026-05-20 16:47:09'),
(10, 6, 10, 'MAIN', NULL, 1, '2026-05-20 16:47:09');

-- --------------------------------------------------------

--
-- Table structure for table `land_parcels`
--

CREATE TABLE `land_parcels` (
  `land_parcel_id` int(11) NOT NULL,
  `land_type_id` int(11) NOT NULL,
  `area_id` int(11) NOT NULL,
  `parcel_number` varchar(50) NOT NULL,
  `map_sheet_number` varchar(50) NOT NULL,
  `area_size` decimal(10,2) NOT NULL,
  `usage_duration` varchar(100) DEFAULT NULL,
  `usage_type` varchar(100) DEFAULT NULL,
  `usage_origin` text DEFAULT NULL,
  `address` text NOT NULL,
  `certificate_number` varchar(50) DEFAULT NULL,
  `gcn_book_number` varchar(50) DEFAULT NULL,
  `notes` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `land_parcels`
--

INSERT INTO `land_parcels` (`land_parcel_id`, `land_type_id`, `area_id`, `parcel_number`, `map_sheet_number`, `area_size`, `usage_duration`, `usage_type`, `usage_origin`, `address`, `certificate_number`, `gcn_book_number`, `notes`) VALUES
(1, 1, 1, '101', '01', 85.50, NULL, NULL, NULL, 'Số 10 Phố Huế', NULL, NULL, NULL),
(2, 1, 3, '205', '05', 120.00, NULL, NULL, NULL, 'Số 20 Kim Mã', NULL, NULL, NULL),
(3, 1, 5, '308', '12', 200.00, NULL, NULL, NULL, 'Số 5 Dịch Vọng', NULL, NULL, NULL),
(4, 2, 9, '412', '20', 350.00, NULL, NULL, NULL, 'Tây Mỗ', NULL, NULL, NULL),
(5, 3, 9, '550', '21', 1500.00, NULL, NULL, NULL, 'Cánh đồng Tây Mỗ', NULL, NULL, NULL),
(6, 1, 2, '102', '01', 60.00, NULL, NULL, NULL, 'Số 15 Hàng Bài', NULL, NULL, NULL),
(7, 1, 4, '206', '05', 90.00, NULL, NULL, NULL, 'Số 30 Đội Cấn', NULL, NULL, NULL),
(8, 1, 6, '309', '12', 150.00, NULL, NULL, NULL, 'Số 8 Mai Dịch', NULL, NULL, NULL),
(9, 9, 1, '888', '02', 500.00, NULL, NULL, NULL, 'Tòa nhà Phố Huế', NULL, NULL, NULL),
(10, 4, 10, '999', '25', 1000.00, NULL, NULL, NULL, 'Mễ Trì', NULL, NULL, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `land_prices`
--

CREATE TABLE `land_prices` (
  `price_id` int(11) NOT NULL,
  `land_type_id` int(11) NOT NULL,
  `area_id` int(11) NOT NULL,
  `unit_price` decimal(18,2) NOT NULL,
  `applied_from` date NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `land_prices`
--

INSERT INTO `land_prices` (`price_id`, `land_type_id`, `area_id`, `unit_price`, `applied_from`) VALUES
(1, 1, 1, 100000000.00, '2026-01-01'),
(2, 1, 3, 80000000.00, '2026-01-01'),
(3, 1, 5, 50000000.00, '2026-01-01'),
(4, 2, 9, 20000000.00, '2026-01-01'),
(5, 3, 9, 500000.00, '2026-01-01'),
(6, 1, 2, 95000000.00, '2026-01-01'),
(7, 1, 4, 75000000.00, '2026-01-01'),
(8, 1, 6, 48000000.00, '2026-01-01'),
(9, 9, 1, 120000000.00, '2026-01-01'),
(10, 4, 10, 1500000.00, '2026-01-01');

-- --------------------------------------------------------

--
-- Table structure for table `land_types`
--

CREATE TABLE `land_types` (
  `land_type_id` int(11) NOT NULL,
  `type_code` varchar(10) NOT NULL,
  `type_name` varchar(150) NOT NULL,
  `is_tax_payment` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `land_types`
--

INSERT INTO `land_types` (`land_type_id`, `type_code`, `type_name`, `is_tax_payment`) VALUES
(1, 'ODT', 'Đất ở tại đô thị', 1),
(2, 'ONT', 'Đất ở tại nông thôn', 1),
(3, 'LUK', 'Đất chuyên trồng lúa nước', 1),
(4, 'CLN', 'Đất trồng cây lâu năm', 1),
(5, 'BHK', 'Đất trồng cây hàng năm khác', 1),
(6, 'NTS', 'Đất nuôi trồng thủy sản', 1),
(7, 'RSX', 'Đất rừng sản xuất', 1),
(8, 'SKC', 'Đất phi nông nghiệp', 1),
(9, 'TMD', 'Đất thương mại, dịch vụ', 1),
(10, 'RPH', 'Đất rừng phòng hộ', 0);

-- --------------------------------------------------------

--
-- Table structure for table `notifications`
--

CREATE TABLE `notifications` (
  `noti_id` int(11) NOT NULL,
  `account_id` int(11) NOT NULL,
  `title` varchar(255) NOT NULL,
  `content` text NOT NULL,
  `noti_type` varchar(50) NOT NULL,
  `is_read` tinyint(1) DEFAULT 0,
  `created_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `notifications`
--

INSERT INTO `notifications` (`noti_id`, `account_id`, `title`, `content`, `noti_type`, `is_read`, `created_at`) VALUES
(1, 4, 'Tiếp nhận', 'Hồ sơ đã tiếp nhận', 'INFO', 0, '2026-05-20 16:47:09'),
(2, 4, 'Nộp thuế', 'Vui lòng thanh toán', 'PAYMENT', 0, '2026-05-20 16:47:09'),
(3, 5, 'Đã duyệt', 'Địa chính xác nhận', 'SUCCESS', 0, '2026-05-20 16:47:09'),
(4, 6, 'Thành công', 'Cảm ơn đã nộp thuế', 'SUCCESS', 0, '2026-05-20 16:47:09'),
(5, 7, 'Quá hạn', 'Hóa đơn quá hạn', 'WARNING', 0, '2026-05-20 16:47:09'),
(6, 8, 'Bổ sung', 'Chụp lại sổ đỏ', 'WARNING', 0, '2026-05-20 16:47:09'),
(7, 9, 'Chào mừng', 'Đăng nhập thành công', 'INFO', 0, '2026-05-20 16:47:09'),
(8, 10, 'Miễn giảm', 'Đã duyệt', 'SUCCESS', 0, '2026-05-20 16:47:09'),
(9, 3, 'Hồ sơ mới', 'Có hồ sơ chờ duyệt', 'WORKFLOW', 0, '2026-05-20 16:47:09'),
(10, 2, 'Đối soát', 'Lô đã hoàn tất', 'WORKFLOW', 0, '2026-05-20 16:47:09');

-- --------------------------------------------------------

--
-- Table structure for table `processing_logs`
--

CREATE TABLE `processing_logs` (
  `plog_id` int(11) NOT NULL,
  `record_id` int(11) NOT NULL,
  `processor_account_id` int(11) NOT NULL,
  `processing_step` varchar(100) NOT NULL,
  `old_status` varchar(50) DEFAULT NULL,
  `new_status` varchar(50) NOT NULL,
  `processor_notes` text DEFAULT NULL,
  `processed_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `processing_logs`
--

INSERT INTO `processing_logs` (`plog_id`, `record_id`, `processor_account_id`, `processing_step`, `old_status`, `new_status`, `processor_notes`, `processed_at`) VALUES
(1, 1, 3, 'Xác nhận', NULL, 'VERIFIED', NULL, '2026-05-20 16:47:09'),
(2, 1, 2, 'Áp thuế', NULL, 'APPROVED', NULL, '2026-05-20 16:47:09'),
(3, 2, 3, 'Xác nhận', NULL, 'VERIFIED', NULL, '2026-05-20 16:47:09'),
(4, 2, 2, 'Áp thuế', NULL, 'APPROVED', NULL, '2026-05-20 16:47:09'),
(5, 3, 3, 'Xác nhận', NULL, 'VERIFIED', NULL, '2026-05-20 16:47:09'),
(6, 3, 2, 'Áp thuế', NULL, 'APPROVED', NULL, '2026-05-20 16:47:09'),
(7, 4, 2, 'Áp thuế', NULL, 'APPROVED', NULL, '2026-05-20 16:47:09'),
(8, 5, 2, 'Áp thuế', NULL, 'APPROVED', NULL, '2026-05-20 16:47:09'),
(9, 6, 3, 'Tiếp nhận', NULL, 'PENDING', NULL, '2026-05-20 16:47:09'),
(10, 7, 2, 'Áp thuế', NULL, 'APPROVED', NULL, '2026-05-20 16:47:09');

-- --------------------------------------------------------

--
-- Table structure for table `reconciliation_batches`
--

CREATE TABLE `reconciliation_batches` (
  `batch_id` int(11) NOT NULL,
  `officer_account_id` int(11) NOT NULL,
  `total_records` int(11) DEFAULT 0,
  `matched_count` int(11) DEFAULT 0,
  `error_count` int(11) DEFAULT 0,
  `batch_notes` text DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `reconciliation_batches`
--

INSERT INTO `reconciliation_batches` (`batch_id`, `officer_account_id`, `total_records`, `matched_count`, `error_count`, `batch_notes`, `created_at`) VALUES
(1, 2, 5, 0, 0, NULL, '2026-05-20 16:47:09'),
(2, 2, 10, 0, 0, NULL, '2026-05-20 16:47:09'),
(3, 2, 2, 0, 0, NULL, '2026-05-20 16:47:09'),
(4, 2, 8, 0, 0, NULL, '2026-05-20 16:47:09'),
(5, 2, 4, 0, 0, NULL, '2026-05-20 16:47:09'),
(6, 2, 6, 0, 0, NULL, '2026-05-20 16:47:09'),
(7, 2, 3, 0, 0, NULL, '2026-05-20 16:47:09'),
(8, 2, 7, 0, 0, NULL, '2026-05-20 16:47:09'),
(9, 2, 1, 0, 0, NULL, '2026-05-20 16:47:09'),
(10, 2, 5, 0, 0, NULL, '2026-05-20 16:47:09');

-- --------------------------------------------------------

--
-- Table structure for table `reconciliation_logs`
--

CREATE TABLE `reconciliation_logs` (
  `log_id` int(11) NOT NULL,
  `transaction_code` varchar(100) NOT NULL,
  `amount_received` decimal(18,2) NOT NULL,
  `bank_trans_id` varchar(100) DEFAULT NULL,
  `webhook_payload` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`webhook_payload`)),
  `status` varchar(20) DEFAULT 'UNMATCHED',
  `created_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `reconciliation_logs`
--

INSERT INTO `reconciliation_logs` (`log_id`, `transaction_code`, `amount_received`, `bank_trans_id`, `webhook_payload`, `status`, `created_at`) VALUES
(1, 'THUE_001', 540000.00, 'FT001', '{\"bank\":\"VCB\",\"amount\":540000}', 'MATCHED', '2026-05-20 16:47:09'),
(2, 'THUE_002', 120000.00, 'FT002', '{\"bank\":\"MB\",\"amount\":120000}', 'MATCHED', '2026-05-20 16:47:09'),
(3, 'THUE_003', 750000.00, 'FT003', '{\"bank\":\"TCB\",\"amount\":750000}', 'MATCHED', '2026-05-20 16:47:09'),
(4, 'THUE_004', 300000.00, 'FT004', '{\"bank\":\"BIDV\",\"amount\":300000}', 'MATCHED', '2026-05-20 16:47:09'),
(5, 'THUE_005', 450000.00, 'FT005', '{\"bank\":\"CTG\",\"amount\":450000}', 'MATCHED', '2026-05-20 16:47:09'),
(6, 'THUE_ERR', 90000.00, 'FT006', '{\"bank\":\"VCB\",\"amount\":90000}', 'UNMATCHED', '2026-05-20 16:47:09'),
(7, 'THUE_007', 820000.00, 'FT007', '{\"bank\":\"ACB\",\"amount\":820000}', 'MATCHED', '2026-05-20 16:47:09'),
(8, 'THUE_008', 150000.00, 'FT008', '{\"bank\":\"MB\",\"amount\":150000}', 'MATCHED', '2026-05-20 16:47:09'),
(9, 'THUE_009', 600000.00, 'FT009', '{\"bank\":\"VPB\",\"amount\":600000}', 'MATCHED', '2026-05-20 16:47:09'),
(10, 'THUE_010', 330000.00, 'FT010', '{\"bank\":\"TCB\",\"amount\":330000}', 'MATCHED', '2026-05-20 16:47:09');

-- --------------------------------------------------------

--
-- Table structure for table `records`
--

CREATE TABLE `records` (
  `record_id` int(11) NOT NULL,
  `citizen_id` int(11) NOT NULL,
  `land_parcel_id` int(11) NOT NULL,
  `record_category` varchar(50) NOT NULL,
  `current_status` varchar(50) DEFAULT 'PENDING',
  `submitted_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `records`
--

INSERT INTO `records` (`record_id`, `citizen_id`, `land_parcel_id`, `record_category`, `current_status`, `submitted_at`) VALUES
(1, 4, 1, 'TAX', 'APPROVED', '2026-05-20 16:47:09'),
(2, 5, 2, 'TAX', 'APPROVED', '2026-05-20 16:47:09'),
(3, 6, 3, 'TAX', 'APPROVED', '2026-05-20 16:47:09'),
(4, 7, 4, 'TAX', 'APPROVED', '2026-05-20 16:47:09'),
(5, 8, 5, 'TAX', 'APPROVED', '2026-05-20 16:47:09'),
(6, 9, 6, 'TAX', 'PENDING', '2026-05-20 16:47:09'),
(7, 10, 7, 'TAX', 'APPROVED', '2026-05-20 16:47:09'),
(8, 4, 8, 'TAX', 'APPROVED', '2026-05-20 16:47:09'),
(9, 5, 9, 'TAX', 'APPROVED', '2026-05-20 16:47:09'),
(10, 6, 10, 'TAX', 'APPROVED', '2026-05-20 16:47:09');

-- --------------------------------------------------------

--
-- Table structure for table `record_documents`
--

CREATE TABLE `record_documents` (
  `document_id` int(11) NOT NULL,
  `record_id` int(11) NOT NULL,
  `file_name` varchar(255) NOT NULL,
  `file_url` text NOT NULL,
  `file_type` varchar(50) DEFAULT NULL,
  `uploaded_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `record_documents`
--

INSERT INTO `record_documents` (`document_id`, `record_id`, `file_name`, `file_url`, `file_type`, `uploaded_at`) VALUES
(1, 1, 'Sodo1.pdf', 'url1', NULL, '2026-05-20 16:47:09'),
(2, 2, 'Sodo2.pdf', 'url2', NULL, '2026-05-20 16:47:09'),
(3, 3, 'Sodo3.pdf', 'url3', NULL, '2026-05-20 16:47:09'),
(4, 4, 'Sodo4.pdf', 'url4', NULL, '2026-05-20 16:47:09'),
(5, 5, 'Sodo5.pdf', 'url5', NULL, '2026-05-20 16:47:09'),
(6, 6, 'Sodo6.pdf', 'url6', NULL, '2026-05-20 16:47:09'),
(7, 7, 'Sodo7.pdf', 'url7', NULL, '2026-05-20 16:47:09'),
(8, 8, 'Sodo8.pdf', 'url8', NULL, '2026-05-20 16:47:09'),
(9, 9, 'Sodo9.pdf', 'url9', NULL, '2026-05-20 16:47:09'),
(10, 10, 'Sodo10.pdf', 'url10', NULL, '2026-05-20 16:47:09');

-- --------------------------------------------------------

--
-- Table structure for table `refresh_tokens`
--

CREATE TABLE `refresh_tokens` (
  `token_id` int(11) NOT NULL,
  `account_id` int(11) NOT NULL,
  `token_value` varchar(255) NOT NULL,
  `expires_at` datetime NOT NULL,
  `is_revoked` tinyint(1) DEFAULT 0,
  `created_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `refresh_tokens`
--

INSERT INTO `refresh_tokens` (`token_id`, `account_id`, `token_value`, `expires_at`, `is_revoked`, `created_at`) VALUES
(1, 1, 'tok-admin', '2026-12-31 00:00:00', 0, '2026-05-20 16:47:09'),
(2, 2, 'tok-tax', '2026-12-31 00:00:00', 0, '2026-05-20 16:47:09'),
(3, 3, 'tok-land', '2026-12-31 00:00:00', 0, '2026-05-20 16:47:09'),
(4, 4, 'tok-cit4', '2026-12-31 00:00:00', 0, '2026-05-20 16:47:09'),
(5, 5, 'tok-cit5', '2026-12-31 00:00:00', 0, '2026-05-20 16:47:09'),
(6, 6, 'tok-cit6', '2026-12-31 00:00:00', 0, '2026-05-20 16:47:09'),
(7, 7, 'tok-cit7', '2026-12-31 00:00:00', 0, '2026-05-20 16:47:09'),
(8, 8, 'tok-cit8', '2026-12-31 00:00:00', 0, '2026-05-20 16:47:09'),
(9, 9, 'tok-cit9', '2026-12-31 00:00:00', 0, '2026-05-20 16:47:09'),
(10, 10, 'tok-cit10', '2026-12-31 00:00:00', 0, '2026-05-20 16:47:09');

-- --------------------------------------------------------

--
-- Table structure for table `roles`
--

CREATE TABLE `roles` (
  `role_id` int(11) NOT NULL,
  `role_code` varchar(50) NOT NULL,
  `role_name` varchar(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `roles`
--

INSERT INTO `roles` (`role_id`, `role_code`, `role_name`) VALUES
(1, 'ROLE_ADMIN', 'Quản trị viên'),
(2, 'ROLE_CITIZEN', 'Công dân'),
(3, 'ROLE_TAX_OFFICER', 'Cán bộ Thuế'),
(4, 'ROLE_LAND_OFFICER', 'Cán bộ Địa chính');

-- --------------------------------------------------------

--
-- Table structure for table `tax_declarations`
--

CREATE TABLE `tax_declarations` (
  `declaration_id` int(11) NOT NULL,
  `record_id` int(11) NOT NULL,
  `declared_area` decimal(10,2) NOT NULL,
  `declared_usage` varchar(100) DEFAULT NULL,
  `declaration_notes` text DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `tax_declarations`
--

INSERT INTO `tax_declarations` (`declaration_id`, `record_id`, `declared_area`, `declared_usage`, `declaration_notes`, `created_at`) VALUES
(1, 1, 85.50, 'Đất ở', NULL, '2026-05-20 16:47:09'),
(2, 2, 120.00, 'Đất ở', NULL, '2026-05-20 16:47:09'),
(3, 3, 200.00, 'Đất ở', NULL, '2026-05-20 16:47:09'),
(4, 4, 350.00, 'Đất ở', NULL, '2026-05-20 16:47:09'),
(5, 5, 1500.00, 'Trồng lúa', NULL, '2026-05-20 16:47:09'),
(6, 6, 60.00, 'Đất ở', NULL, '2026-05-20 16:47:09'),
(7, 7, 90.00, 'Đất ở', NULL, '2026-05-20 16:47:09'),
(8, 8, 150.00, 'Đất ở', NULL, '2026-05-20 16:47:09'),
(9, 9, 500.00, 'TMDV', NULL, '2026-05-20 16:47:09'),
(10, 10, 1000.00, 'Trồng cây', NULL, '2026-05-20 16:47:09');

-- --------------------------------------------------------

--
-- Table structure for table `tax_exempt_subjects`
--

CREATE TABLE `tax_exempt_subjects` (
  `exempt_id` int(11) NOT NULL,
  `citizen_id` int(11) NOT NULL,
  `uploaded_by_account` int(11) NOT NULL,
  `full_name` varchar(100) NOT NULL,
  `exemption_reason` text DEFAULT NULL,
  `discount_rate` decimal(5,2) NOT NULL,
  `applied_year` int(11) NOT NULL,
  `uploaded_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `tax_exempt_subjects`
--

INSERT INTO `tax_exempt_subjects` (`exempt_id`, `citizen_id`, `uploaded_by_account`, `full_name`, `exemption_reason`, `discount_rate`, `applied_year`, `uploaded_at`) VALUES
(1, 4, 1, 'Nguyễn Văn Bình', NULL, 50.00, 2026, '2026-05-20 16:47:09'),
(2, 5, 1, 'Lý Nhã Kỳ', NULL, 100.00, 2026, '2026-05-20 16:47:09'),
(3, 6, 1, 'Phạm Xuân Ẩn', NULL, 50.00, 2026, '2026-05-20 16:47:09'),
(4, 7, 1, 'Vũ Trọng Phụng', NULL, 100.00, 2026, '2026-05-20 16:47:09'),
(5, 8, 1, 'Hồ Xuân Hương', NULL, 100.00, 2026, '2026-05-20 16:47:09'),
(6, 9, 1, 'Đinh Bộ Lĩnh', NULL, 50.00, 2026, '2026-05-20 16:47:09'),
(7, 10, 1, 'Trần Hưng Đạo', NULL, 50.00, 2026, '2026-05-20 16:47:09'),
(8, 4, 1, 'Nguyễn Văn Bình (Cũ)', NULL, 50.00, 2025, '2026-05-20 16:47:09'),
(9, 5, 1, 'Lý Nhã Kỳ (Cũ)', NULL, 100.00, 2025, '2026-05-20 16:47:09'),
(10, 6, 1, 'Phạm Xuân Ẩn (Cũ)', NULL, 50.00, 2025, '2026-05-20 16:47:09');

-- --------------------------------------------------------

--
-- Table structure for table `tax_payments`
--

CREATE TABLE `tax_payments` (
  `pay_id` int(11) NOT NULL,
  `record_id` int(11) DEFAULT NULL,
  `land_parcel_id` int(11) NOT NULL,
  `tax_year` int(11) NOT NULL,
  `total_amount_due` decimal(18,2) NOT NULL,
  `due_date` date NOT NULL,
  `payment_status` varchar(20) DEFAULT 'UNPAID',
  `transaction_code` varchar(100) DEFAULT NULL,
  `paid_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `tax_payments`
--

INSERT INTO `tax_payments` (`pay_id`, `record_id`, `land_parcel_id`, `tax_year`, `total_amount_due`, `due_date`, `payment_status`, `transaction_code`, `paid_at`) VALUES
(1, 1, 1, 2026, 540000.00, '2026-12-31', 'PAID', 'THUE_001', NULL),
(2, 2, 2, 2026, 120000.00, '2026-12-31', 'PAID', 'THUE_002', NULL),
(3, 3, 3, 2026, 750000.00, '2026-12-31', 'PAID', 'THUE_003', NULL),
(4, 4, 4, 2026, 300000.00, '2026-12-31', 'PAID', 'THUE_004', NULL),
(5, 5, 5, 2026, 450000.00, '2026-12-31', 'PAID', 'THUE_005', NULL),
(6, NULL, 6, 2026, 90000.00, '2026-12-31', 'UNPAID', 'THUE_ERR', NULL),
(7, 7, 7, 2026, 820000.00, '2026-12-31', 'PAID', 'THUE_007', NULL),
(8, 8, 8, 2026, 150000.00, '2026-12-31', 'PAID', 'THUE_008', NULL),
(9, 9, 9, 2026, 600000.00, '2026-12-31', 'PAID', 'THUE_009', NULL),
(10, 10, 10, 2026, 330000.00, '2026-12-31', 'PAID', 'THUE_010', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `tax_payment_details`
--

CREATE TABLE `tax_payment_details` (
  `detail_id` int(11) NOT NULL,
  `pay_id` int(11) NOT NULL,
  `rate_id` int(11) NOT NULL,
  `calculated_area` decimal(10,2) NOT NULL,
  `applied_tax_rate` decimal(5,4) NOT NULL,
  `applied_unit_price` decimal(18,2) NOT NULL,
  `line_amount` decimal(18,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `tax_payment_details`
--

INSERT INTO `tax_payment_details` (`detail_id`, `pay_id`, `rate_id`, `calculated_area`, `applied_tax_rate`, `applied_unit_price`, `line_amount`) VALUES
(1, 1, 1, 85.50, 0.0003, 100000000.00, 540000.00),
(2, 2, 1, 120.00, 0.0003, 80000000.00, 120000.00),
(3, 3, 1, 200.00, 0.0003, 50000000.00, 750000.00),
(4, 4, 1, 350.00, 0.0003, 20000000.00, 300000.00),
(5, 5, 4, 1500.00, 0.0001, 500000.00, 450000.00),
(6, 6, 1, 60.00, 0.0003, 95000000.00, 90000.00),
(7, 7, 1, 90.00, 0.0003, 75000000.00, 820000.00),
(8, 8, 1, 150.00, 0.0003, 48000000.00, 150000.00),
(9, 9, 7, 500.00, 0.0006, 120000000.00, 600000.00),
(10, 10, 4, 1000.00, 0.0001, 1500000.00, 330000.00);

-- --------------------------------------------------------

--
-- Table structure for table `tax_rates`
--

CREATE TABLE `tax_rates` (
  `rate_id` int(11) NOT NULL,
  `tax_name` varchar(100) NOT NULL,
  `rate_value` decimal(5,4) NOT NULL,
  `rate_code` varchar(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `tax_rates`
--

INSERT INTO `tax_rates` (`rate_id`, `tax_name`, `rate_value`, `rate_code`) VALUES
(1, 'Trong hạn mức', 0.0003, 'TIER_1'),
(2, 'Vượt <= 3 lần HM', 0.0007, 'TIER_2'),
(3, 'Vượt > 3 lần HM', 0.0015, 'TIER_3'),
(4, 'Nông nghiệp bậc 1', 0.0001, 'AGRI_1'),
(5, 'Nông nghiệp bậc 2', 0.0002, 'AGRI_2'),
(6, 'Phi nông nghiệp', 0.0005, 'PROD_1'),
(7, 'Thương mại', 0.0006, 'COM_1'),
(8, 'Rừng sản xuất', 0.0001, 'FOR_1'),
(9, 'Thủy sản', 0.0002, 'AQUA_1'),
(10, 'Phạt nộp chậm', 0.0005, 'LATE_FEE');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `accounts`
--
ALTER TABLE `accounts`
  ADD PRIMARY KEY (`account_id`),
  ADD KEY `citizen_id` (`citizen_id`),
  ADD KEY `role_id` (`role_id`);

--
-- Indexes for table `areas`
--
ALTER TABLE `areas`
  ADD PRIMARY KEY (`area_id`);

--
-- Indexes for table `citizen_local`
--
ALTER TABLE `citizen_local`
  ADD PRIMARY KEY (`citizen_id`),
  ADD UNIQUE KEY `cccd_number` (`cccd_number`);

--
-- Indexes for table `complaints`
--
ALTER TABLE `complaints`
  ADD PRIMARY KEY (`complaint_id`),
  ADD KEY `fk_complaint_citizen` (`citizen_id`),
  ADD KEY `fk_complaint_record` (`record_id`);

--
-- Indexes for table `land_owners`
--
ALTER TABLE `land_owners`
  ADD PRIMARY KEY (`ownership_id`),
  ADD KEY `citizen_id` (`citizen_id`),
  ADD KEY `land_parcel_id` (`land_parcel_id`);

--
-- Indexes for table `land_parcels`
--
ALTER TABLE `land_parcels`
  ADD PRIMARY KEY (`land_parcel_id`),
  ADD KEY `land_type_id` (`land_type_id`),
  ADD KEY `area_id` (`area_id`);

--
-- Indexes for table `land_prices`
--
ALTER TABLE `land_prices`
  ADD PRIMARY KEY (`price_id`),
  ADD KEY `land_type_id` (`land_type_id`),
  ADD KEY `area_id` (`area_id`);

--
-- Indexes for table `land_types`
--
ALTER TABLE `land_types`
  ADD PRIMARY KEY (`land_type_id`),
  ADD UNIQUE KEY `type_code` (`type_code`);

--
-- Indexes for table `notifications`
--
ALTER TABLE `notifications`
  ADD PRIMARY KEY (`noti_id`),
  ADD KEY `account_id` (`account_id`);

--
-- Indexes for table `processing_logs`
--
ALTER TABLE `processing_logs`
  ADD PRIMARY KEY (`plog_id`),
  ADD KEY `record_id` (`record_id`),
  ADD KEY `processor_account_id` (`processor_account_id`);

--
-- Indexes for table `reconciliation_batches`
--
ALTER TABLE `reconciliation_batches`
  ADD PRIMARY KEY (`batch_id`),
  ADD KEY `officer_account_id` (`officer_account_id`);

--
-- Indexes for table `reconciliation_logs`
--
ALTER TABLE `reconciliation_logs`
  ADD PRIMARY KEY (`log_id`),
  ADD UNIQUE KEY `bank_trans_id` (`bank_trans_id`);

--
-- Indexes for table `records`
--
ALTER TABLE `records`
  ADD PRIMARY KEY (`record_id`),
  ADD KEY `citizen_id` (`citizen_id`),
  ADD KEY `land_parcel_id` (`land_parcel_id`);

--
-- Indexes for table `record_documents`
--
ALTER TABLE `record_documents`
  ADD PRIMARY KEY (`document_id`),
  ADD KEY `record_id` (`record_id`);

--
-- Indexes for table `refresh_tokens`
--
ALTER TABLE `refresh_tokens`
  ADD PRIMARY KEY (`token_id`),
  ADD UNIQUE KEY `token_value` (`token_value`),
  ADD KEY `account_id` (`account_id`);

--
-- Indexes for table `roles`
--
ALTER TABLE `roles`
  ADD PRIMARY KEY (`role_id`),
  ADD UNIQUE KEY `role_code` (`role_code`);

--
-- Indexes for table `tax_declarations`
--
ALTER TABLE `tax_declarations`
  ADD PRIMARY KEY (`declaration_id`),
  ADD UNIQUE KEY `record_id` (`record_id`);

--
-- Indexes for table `tax_exempt_subjects`
--
ALTER TABLE `tax_exempt_subjects`
  ADD PRIMARY KEY (`exempt_id`),
  ADD KEY `citizen_id` (`citizen_id`),
  ADD KEY `uploaded_by_account` (`uploaded_by_account`);

--
-- Indexes for table `tax_payments`
--
ALTER TABLE `tax_payments`
  ADD PRIMARY KEY (`pay_id`),
  ADD UNIQUE KEY `transaction_code` (`transaction_code`),
  ADD KEY `record_id` (`record_id`),
  ADD KEY `land_parcel_id` (`land_parcel_id`);

--
-- Indexes for table `tax_payment_details`
--
ALTER TABLE `tax_payment_details`
  ADD PRIMARY KEY (`detail_id`),
  ADD KEY `pay_id` (`pay_id`),
  ADD KEY `rate_id` (`rate_id`);

--
-- Indexes for table `tax_rates`
--
ALTER TABLE `tax_rates`
  ADD PRIMARY KEY (`rate_id`),
  ADD UNIQUE KEY `rate_code` (`rate_code`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `accounts`
--
ALTER TABLE `accounts`
  MODIFY `account_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `areas`
--
ALTER TABLE `areas`
  MODIFY `area_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `citizen_local`
--
ALTER TABLE `citizen_local`
  MODIFY `citizen_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `complaints`
--
ALTER TABLE `complaints`
  MODIFY `complaint_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `land_owners`
--
ALTER TABLE `land_owners`
  MODIFY `ownership_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `land_parcels`
--
ALTER TABLE `land_parcels`
  MODIFY `land_parcel_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `land_prices`
--
ALTER TABLE `land_prices`
  MODIFY `price_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `land_types`
--
ALTER TABLE `land_types`
  MODIFY `land_type_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `notifications`
--
ALTER TABLE `notifications`
  MODIFY `noti_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `processing_logs`
--
ALTER TABLE `processing_logs`
  MODIFY `plog_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `reconciliation_batches`
--
ALTER TABLE `reconciliation_batches`
  MODIFY `batch_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `reconciliation_logs`
--
ALTER TABLE `reconciliation_logs`
  MODIFY `log_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `records`
--
ALTER TABLE `records`
  MODIFY `record_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `record_documents`
--
ALTER TABLE `record_documents`
  MODIFY `document_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `refresh_tokens`
--
ALTER TABLE `refresh_tokens`
  MODIFY `token_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `roles`
--
ALTER TABLE `roles`
  MODIFY `role_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `tax_declarations`
--
ALTER TABLE `tax_declarations`
  MODIFY `declaration_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `tax_exempt_subjects`
--
ALTER TABLE `tax_exempt_subjects`
  MODIFY `exempt_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `tax_payments`
--
ALTER TABLE `tax_payments`
  MODIFY `pay_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `tax_payment_details`
--
ALTER TABLE `tax_payment_details`
  MODIFY `detail_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `tax_rates`
--
ALTER TABLE `tax_rates`
  MODIFY `rate_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `accounts`
--
ALTER TABLE `accounts`
  ADD CONSTRAINT `accounts_ibfk_1` FOREIGN KEY (`citizen_id`) REFERENCES `citizen_local` (`citizen_id`),
  ADD CONSTRAINT `accounts_ibfk_2` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`);

--
-- Constraints for table `complaints`
--
ALTER TABLE `complaints`
  ADD CONSTRAINT `fk_complaint_citizen` FOREIGN KEY (`citizen_id`) REFERENCES `citizen_local` (`citizen_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_complaint_record` FOREIGN KEY (`record_id`) REFERENCES `records` (`record_id`) ON DELETE SET NULL;

--
-- Constraints for table `land_owners`
--
ALTER TABLE `land_owners`
  ADD CONSTRAINT `land_owners_ibfk_1` FOREIGN KEY (`citizen_id`) REFERENCES `citizen_local` (`citizen_id`),
  ADD CONSTRAINT `land_owners_ibfk_2` FOREIGN KEY (`land_parcel_id`) REFERENCES `land_parcels` (`land_parcel_id`);

--
-- Constraints for table `land_parcels`
--
ALTER TABLE `land_parcels`
  ADD CONSTRAINT `land_parcels_ibfk_1` FOREIGN KEY (`land_type_id`) REFERENCES `land_types` (`land_type_id`),
  ADD CONSTRAINT `land_parcels_ibfk_2` FOREIGN KEY (`area_id`) REFERENCES `areas` (`area_id`);

--
-- Constraints for table `land_prices`
--
ALTER TABLE `land_prices`
  ADD CONSTRAINT `land_prices_ibfk_1` FOREIGN KEY (`land_type_id`) REFERENCES `land_types` (`land_type_id`),
  ADD CONSTRAINT `land_prices_ibfk_2` FOREIGN KEY (`area_id`) REFERENCES `areas` (`area_id`);

--
-- Constraints for table `notifications`
--
ALTER TABLE `notifications`
  ADD CONSTRAINT `notifications_ibfk_1` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`account_id`);

--
-- Constraints for table `processing_logs`
--
ALTER TABLE `processing_logs`
  ADD CONSTRAINT `processing_logs_ibfk_1` FOREIGN KEY (`record_id`) REFERENCES `records` (`record_id`),
  ADD CONSTRAINT `processing_logs_ibfk_2` FOREIGN KEY (`processor_account_id`) REFERENCES `accounts` (`account_id`);

--
-- Constraints for table `reconciliation_batches`
--
ALTER TABLE `reconciliation_batches`
  ADD CONSTRAINT `reconciliation_batches_ibfk_1` FOREIGN KEY (`officer_account_id`) REFERENCES `accounts` (`account_id`);

--
-- Constraints for table `records`
--
ALTER TABLE `records`
  ADD CONSTRAINT `records_ibfk_1` FOREIGN KEY (`citizen_id`) REFERENCES `citizen_local` (`citizen_id`),
  ADD CONSTRAINT `records_ibfk_2` FOREIGN KEY (`land_parcel_id`) REFERENCES `land_parcels` (`land_parcel_id`);

--
-- Constraints for table `record_documents`
--
ALTER TABLE `record_documents`
  ADD CONSTRAINT `record_documents_ibfk_1` FOREIGN KEY (`record_id`) REFERENCES `records` (`record_id`);

--
-- Constraints for table `refresh_tokens`
--
ALTER TABLE `refresh_tokens`
  ADD CONSTRAINT `refresh_tokens_ibfk_1` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`account_id`);

--
-- Constraints for table `tax_declarations`
--
ALTER TABLE `tax_declarations`
  ADD CONSTRAINT `tax_declarations_ibfk_1` FOREIGN KEY (`record_id`) REFERENCES `records` (`record_id`);

--
-- Constraints for table `tax_exempt_subjects`
--
ALTER TABLE `tax_exempt_subjects`
  ADD CONSTRAINT `tax_exempt_subjects_ibfk_1` FOREIGN KEY (`citizen_id`) REFERENCES `citizen_local` (`citizen_id`),
  ADD CONSTRAINT `tax_exempt_subjects_ibfk_2` FOREIGN KEY (`uploaded_by_account`) REFERENCES `accounts` (`account_id`);

--
-- Constraints for table `tax_payments`
--
ALTER TABLE `tax_payments`
  ADD CONSTRAINT `tax_payments_ibfk_1` FOREIGN KEY (`record_id`) REFERENCES `records` (`record_id`),
  ADD CONSTRAINT `tax_payments_ibfk_2` FOREIGN KEY (`land_parcel_id`) REFERENCES `land_parcels` (`land_parcel_id`);

--
-- Constraints for table `tax_payment_details`
--
ALTER TABLE `tax_payment_details`
  ADD CONSTRAINT `tax_payment_details_ibfk_1` FOREIGN KEY (`pay_id`) REFERENCES `tax_payments` (`pay_id`),
  ADD CONSTRAINT `tax_payment_details_ibfk_2` FOREIGN KEY (`rate_id`) REFERENCES `tax_rates` (`rate_id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
