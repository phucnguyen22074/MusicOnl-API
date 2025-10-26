package com.example.demo.configuration;

import java.util.HashSet;
import java.util.Set;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.example.demo.dto.ArtistsDTO;
import com.example.demo.dto.GenresDTO;
import com.example.demo.dto.PlaylistDTO;
import com.example.demo.dto.SongsDTO;
import com.example.demo.dto.UsersDTO;
import com.example.demo.entities.Albums;
import com.example.demo.entities.Artists;
import com.example.demo.entities.Genres;
import com.example.demo.entities.Playlists;
import com.example.demo.entities.Playlistsongs;
import com.example.demo.entities.Songs;
import com.example.demo.entities.Users;

@Configuration
public class ModelMapperConfiguration {

	@Autowired
    private Environment environment; // lấy giá trị từ application.properties

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        // Lấy base_url từ properties
        String imageBaseUrl = environment.getProperty("images_url");
        String artistBaseUrl = environment.getProperty("artist_images");

        // ================= Users -> UsersDTO =================
        mapper.addMappings(new PropertyMap<Users, UsersDTO>() {
            @Override
            protected void configure() {
                map().setUserId(source.getUserId());
                map().setUsername(source.getUsername());
                map().setEmail(source.getEmail());
                map().setFullName(source.getFullName());
                map().setRole(source.getRole());
                map().setCreatedAt(source.getCreatedAt());
                map().setPassword(source.getPassword());

                using(ctx -> {
                    String avatar = (String) ctx.getSource();
                    return (avatar != null && !avatar.isEmpty())
                            ? imageBaseUrl + avatar
                            : null;
                }).map(source.getAvatarUrl(), destination.getAvatarUrl());
            }
        });

        // ================= UsersDTO -> Users =================
        mapper.addMappings(new PropertyMap<UsersDTO, Users>() {
            @Override
            protected void configure() {
                map().setUserId(source.getUserId());
                map().setUsername(source.getUsername());
                map().setEmail(source.getEmail());
                map().setFullName(source.getFullName());
                map().setRole(source.getRole());
                map().setCreatedAt(source.getCreatedAt());
                map().setPassword(source.getPassword());

                using(ctx -> {
                    String avatar = (String) ctx.getSource();
                    if (avatar != null && avatar.startsWith(imageBaseUrl)) {
                        return avatar.replace(imageBaseUrl, "");
                    }
                    return avatar;
                }).map(source.getAvatarUrl(), destination.getAvatarUrl());
            }
        });

        // ================= Artists -> ArtistsDTO =================
        mapper.addMappings(new PropertyMap<Artists, ArtistsDTO>() {
            @Override
            protected void configure() {
                map().setArtistId(source.getArtistId());
                map().setName(source.getName());
                map().setBio(source.getBio());
                map().setCreatedAt(source.getCreatedAt());
                map().setImage(source.getImageUrl());
                using(ctx -> {
                    String img = (String) ctx.getSource();
                    return (img != null && !img.isEmpty())
                            ? artistBaseUrl + img
                            : null;
                }).map(source.getImageUrl(), destination.getImageUrl());
            }
        });

        // ================= ArtistsDTO -> Artists =================
        mapper.addMappings(new PropertyMap<ArtistsDTO, Artists>() {
            @Override
            protected void configure() {
                map().setArtistId(source.getArtistId());
                map().setName(source.getName());
                map().setBio(source.getBio());
                map().setCreatedAt(source.getCreatedAt());
                using(ctx -> {
                    String img = (String) ctx.getSource();
                    if (img != null && img.startsWith(artistBaseUrl)) {
                        return img.replace(artistBaseUrl, "");
                    }
                    return img;
                }).map(source.getImageUrl(), destination.getImageUrl());
            }
        });
        
     // ================= Songs -> SongsDTO =================
        mapper.addMappings(new PropertyMap<Songs, SongsDTO>() {
            @Override
            protected void configure() {
                map().setSongId(source.getSongId());
                map().setTitle(source.getTitle());
                map().setDuration(source.getDuration());
                map().setLyrics(source.getLyrics());
                map().setReleaseDate(source.getReleaseDate());
                map().setStatus(source.getStatus());
                map().setCreatedAt(source.getCreatedAt());

                // FilePath và Image thêm prefix URL
                using(ctx -> {
                    String file = (String) ctx.getSource();
                    return (file != null && !file.isEmpty())
                            ? environment.getProperty("musics_url") + file
                            : null;
                }).map(source.getFilePath(), destination.getFilePath());

                using(ctx -> {
                    String img = (String) ctx.getSource();
                    return (img != null && !img.isEmpty())
                            ? environment.getProperty("images_url") + img
                            : null;
                }).map(source.getImageUrl(), destination.getImageUrl());

                // Album
                using(ctx -> {
                    Albums album = (Albums) ctx.getSource();
                    return album != null ? album.getAlbumId() : null;
                }).map(source.getAlbums(), destination.getAlbumId());

                using(ctx -> {
                    Albums album = (Albums) ctx.getSource();
                    return album != null ? album.getTitle() : null;
                }).map(source.getAlbums(), destination.getAlbumTitle());

                // User
                using(ctx -> {
                    Users user = (Users) ctx.getSource();
                    return user != null ? user.getUserId() : null;
                }).map(source.getUsers(), destination.getUserId());

                using(ctx -> {
                    Users user = (Users) ctx.getSource();
                    return user != null ? user.getUsername() : null;
                }).map(source.getUsers(), destination.getUsername());

                // Genres: chỉ lấy tên
                using(ctx -> {
                    Set<Genres> genres = (Set<Genres>) ctx.getSource();
                    if (genres == null) return null;
                    Set<String> names = new HashSet<>();
                    for (Genres g : genres) {
                        names.add(g.getName());
                    }
                    return names;
                }).map(source.getGenreses(), destination.getGenres());

                // Artists: ModelMapper tự map sang ArtistsDTO
                using(ctx -> {
                    Set<Artists> artists = (Set<Artists>) ctx.getSource();
                    if (artists == null) return null;
                    Set<ArtistsDTO> dtos = new HashSet<>();
                    for (Artists a : artists) {
                        dtos.add(mapper.map(a, ArtistsDTO.class)); // ModelMapper map từng Artist sang DTO
                    }
                    return dtos;
                }).map(source.getartists(), destination.getArtists());

            }
        });

        // ================= SongsDTO -> Songs =================
        mapper.addMappings(new PropertyMap<SongsDTO, Songs>() {
            @Override
            protected void configure() {
                map().setSongId(source.getSongId());
                map().setTitle(source.getTitle());
                map().setDuration(source.getDuration());
                map().setLyrics(source.getLyrics());
                map().setReleaseDate(source.getReleaseDate());
                map().setStatus(source.getStatus());
                map().setCreatedAt(source.getCreatedAt());

                // FilePath bỏ prefix khi lưu
                using(ctx -> {
                    String file = (String) ctx.getSource();
                    String base = environment.getProperty("musics_url");
                    if (file != null && file.startsWith(base)) {
                        return file.replace(base, "");
                    }
                    return file;
                }).map(source.getFilePath(), destination.getFilePath());

                // ImageUrl bỏ prefix khi lưu
                using(ctx -> {
                    String img = (String) ctx.getSource();
                    String base = environment.getProperty("images_url");
                    if (img != null && img.startsWith(base)) {
                        return img.replace(base, "");
                    }
                    return img;
                }).map(source.getImageUrl(), destination.getImageUrl());
            }
        });

     // ================= Genres -> GenresDTO =================
        mapper.addMappings(new PropertyMap<Genres, GenresDTO>() {
            @Override
            protected void configure() {
                map().setGenreId(source.getGenreId());
                map().setName(source.getName());
            }
        });

        // ================= GenresDTO -> Genres =================
        mapper.addMappings(new PropertyMap<GenresDTO, Genres>() {
            @Override
            protected void configure() {
                map().setGenreId(source.getGenreId());
                map().setName(source.getName());
            }
        });
        
     // ================= Playlists -> PlaylistDTO =================
        mapper.addMappings(new PropertyMap<Playlists, PlaylistDTO>() {
            @Override
            protected void configure() {
                map().setPlaylistId(source.getPlaylistId());
                map().setName(source.getName());
                map().setDescription(source.getDescription());
                map().setCreatedAt(source.getCreatedAt());

                // Cover image URL
                using(ctx -> {
                    String img = (String) ctx.getSource();
                    return (img != null && !img.isEmpty())
                            ? environment.getProperty("playlist_images") + img
                            : null;
                }).map(source.getCoverImage(), destination.getCoverImage());

                // User
                using(ctx -> {
                    if (source.getUsers() != null)
                        return source.getUsers().getUserId();
                    return null;
                }).map(source.getUsers(), destination.getUserId());

                using(ctx -> {
                    if (source.getUsers() != null)
                        return source.getUsers().getUsername();
                    return null;
                }).map(source.getUsers(), destination.getUsername());

                // Songs (qua Playlistsongs)
                using(ctx -> {
                    Set<Playlistsongs> playlistSongs = (Set<Playlistsongs>) ctx.getSource();
                    if (playlistSongs == null) return null;

                    Set<SongsDTO> dtos = new HashSet<>();
                    for (Playlistsongs ps : playlistSongs) {
                        if (ps.getSongs() != null) {
                            dtos.add(mapper.map(ps.getSongs(), SongsDTO.class));
                        }
                    }
                    return dtos;
                }).map(source.getPlaylistsongses(), destination.getSongs());
            }
        });


        // ================= PlaylistDTO -> Playlists =================
        mapper.addMappings(new PropertyMap<PlaylistDTO, Playlists>() {
            @Override
            protected void configure() {
                map().setPlaylistId(source.getPlaylistId());
                map().setName(source.getName());
                map().setDescription(source.getDescription());
                map().setCreatedAt(source.getCreatedAt());

                // Remove base URL for cover
                using(ctx -> {
                    String img = (String) ctx.getSource();
                    String base = environment.getProperty("playlist_images");
                    if (img != null && img.startsWith(base)) {
                        return img.replace(base, "");
                    }
                    return img;
                }).map(source.getCoverImage(), destination.getCoverImage());
            }
        });


        return mapper;
    }
}

